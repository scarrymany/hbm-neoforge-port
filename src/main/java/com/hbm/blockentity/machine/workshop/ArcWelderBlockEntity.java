package com.hbm.blockentity.machine.workshop;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.workshop.ArcWelderMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes.ArcWelderRecipe;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityMachineArcWelder}: maxPower 2_000 (grows with recipe), 3 inputs + out + battery.
 * Upgrades skipped — base duration/consumption.
 */
public class ArcWelderBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    public static final int SLOT_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final long BASE_MAX = 2_000L;
    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM tank;
    public long power;
    public int progress;
    public int processTime = 1;
    public long consumption;
    public boolean isProcessing;

    public ArcWelderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, true);
        tank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcWelder");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        return slot < SLOT_OUT;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / Math.max(1, processTime);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, getMaxPower());
        for (Direction d : Direction.values()) {
            trySubscribe(level, worldPosition.relative(d), d);
            if (tank.getTankType() != Fluids.NONE) {
                trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(d), d));
            }
        }

        ArcWelderRecipe recipe = ArcWelderRecipes.getRecipe(
                inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2));
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

    private boolean hasFluid(ArcWelderRecipe recipe) {
        if (recipe.fluid == null) return true;
        return tank.getTankType() == recipe.fluid.type && tank.getFill() >= recipe.fluid.fill;
    }

    private boolean canOutput(ArcWelderRecipe recipe) {
        return inventory.insertItem(SLOT_OUT, recipe.output.copy(), true).isEmpty();
    }

    private void consume(ArcWelderRecipe recipe) {
        List<AStack> left = new ArrayList<>(List.of(recipe.ingredients));
        for (int i = 0; i < 3 && !left.isEmpty(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            for (int j = 0; j < left.size(); j++) {
                AStack key = left.get(j);
                if (key.matchesRecipe(stack, true) && stack.getCount() >= key.count()) {
                    inventory.extractItem(i, key.count(), false);
                    left.remove(j);
                    break;
                }
            }
        }
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
        return new ArcWelderMenu(id, inv, this);
    }
}
