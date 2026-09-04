package com.hbm.blockentity.machine.workshop;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.workshop.SolderingMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolderingRecipes.SolderingRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * CE {@code TileEntityMachineSolderingStation}: maxPower 2_000, slots 0-2 toppings / 3-4 pcb / 5 solder.
 * {@code tank.setType(8)} Exact CE {@code :123}. Slot 8 Exact CE
 * {@code ContainerMachineSolderingStation.java:38}. Upgrades 9-10 skipped.
 */
public class SolderingBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    public static final int SLOT_OUT = 6;
    public static final int SLOT_BATTERY = 7;
    public static final int SLOT_ID = 8;
    public static final long BASE_MAX = 2_000L;
    public static final int TANK_CAPACITY = 8_000;

    public final FluidTankNTM tank;
    public long power;
    public int progress;
    public int processTime = 1;
    public long consumption;
    public boolean isProcessing;

    public SolderingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 9, true, true);
        tank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSolderingStation");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        // CE :281-301 returns false for slot 8; without this the ID never lands and setType is dead.
        if (slot == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        return slot < SLOT_OUT;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / Math.max(1, processTime);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, getMaxPower());
        // CE TileEntityMachineSolderingStation.java:123
        this.tank.setType(SLOT_ID, inventory);
        for (Direction d : Direction.values()) {
            trySubscribe(level, worldPosition.relative(d), d);
            if (tank.getTankType() != Fluids.NONE) {
                trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(d), d));
            }
        }

        ItemStack[] ins = new ItemStack[6];
        for (int i = 0; i < 6; i++) ins[i] = inventory.getStackInSlot(i);
        SolderingRecipe recipe = SolderingRecipes.getRecipe(ins);
        if (recipe == null || !canOutput(recipe) || !hasFluid(recipe)) {
            progress = 0;
            isProcessing = false;
            consumption = 0;
            return;
        }
        processTime = recipe.duration;
        consumption = recipe.consumption;
        if (power < consumption) {
            isProcessing = false;
            return;
        }
        isProcessing = true;
        power -= consumption;
        progress++;
        if (progress >= processTime) {
            consume(recipe);
            inventory.insertItem(SLOT_OUT, recipe.output.copy(), false);
            progress = 0;
        }
    }

    private boolean hasFluid(SolderingRecipe recipe) {
        if (recipe.fluid == null) return true;
        return tank.getTankType() == recipe.fluid.type && tank.getFill() >= recipe.fluid.fill;
    }

    private boolean canOutput(SolderingRecipe recipe) {
        return inventory.insertItem(SLOT_OUT, recipe.output.copy(), true).isEmpty();
    }

    private void consumeGroup(AStack[] keys, int start, int len) {
        boolean[] used = new boolean[len];
        for (AStack key : keys) {
            for (int i = 0; i < len; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(start + i), false)) {
                    inventory.extractItem(start + i, key.count(), false);
                    used[i] = true;
                    break;
                }
            }
        }
    }

    private void consume(SolderingRecipe recipe) {
        consumeGroup(recipe.toppings, 0, 3);
        consumeGroup(recipe.pcb, 3, 2);
        consumeGroup(recipe.solder, 5, 1);
        if (recipe.fluid != null) tank.setFill(tank.getFill() - recipe.fluid.fill);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return Math.max(BASE_MAX, consumption * 100L);
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        if (tank.getPressure() != pressure) return 0;
        if (tank.getTankType() == type || (tank.getTankType() == Fluids.NONE && tank.getFill() == 0)) {
            return tank.getMaxFill() - tank.getFill();
        }
        return 0;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        if (tank.getPressure() != pressure) return amount;
        if (tank.getTankType() == Fluids.NONE && tank.getFill() == 0) tank.setTankType(type);
        if (tank.getTankType() != type) return amount;
        int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
        tank.setFill(tank.getFill() + toAdd);
        return amount - toAdd;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
        buf.writeInt(processTime);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        power = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
        processTime = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tank.writeToNBT(nbt, "nt");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tank.readFromNBT(nbt, "nt");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SolderingMenu(id, inv, this);
    }
}
