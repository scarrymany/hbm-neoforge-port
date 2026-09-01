package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.dummyable.RockMillMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.RockMillRecipes.RockMillRecipe;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineRockMill} without AE2/blueprint module — auto-detect first match.
 */
public class MachineRockMillBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_IN_START = 2;
    public static final int SLOT_OUT_START = 5;

    public final FluidTankNTM inputTank;
    public final FluidTankNTM outputTank;
    public long power;
    public long maxPower = 2_500;
    public boolean didProcess;
    public int progress;
    public int processTime = 1;

    public MachineRockMillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, true, true);
        this.inputTank = new FluidTankNTM(Fluids.WATER, 4_000).withOwner(this);
        this.outputTank = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRockMill");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        if (slot >= SLOT_IN_START && slot < SLOT_OUT_START) return RockMillRecipes.isIngredient(stack);
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= SLOT_OUT_START;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{2, 3, 4, 5, 6, 7};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, getMaxPower());
        for (DirPos pos : getConPos()) {
            trySubscribe(level, pos);
            if (inputTank.getTankType() != Fluids.NONE) trySubscribe(inputTank.getTankType(), level, pos);
            if (outputTank.getFill() > 0) tryProvide(outputTank, level, pos);
        }

        ItemStack[] ins = new ItemStack[]{
                inventory.getStackInSlot(2), inventory.getStackInSlot(3), inventory.getStackInSlot(4)
        };
        FluidStack tankSnap = new FluidStack(inputTank.getTankType(), inputTank.getFill());
        RockMillRecipe recipe = RockMillRecipes.find(ins, tankSnap);
        didProcess = false;
        if (recipe == null) {
            progress = 0;
            processTime = 1;
        } else {
            processTime = recipe.duration;
            maxPower = Math.max(2_500L, recipe.power * 100L);
            if (power >= recipe.power && canOutput(recipe)) {
                power -= recipe.power;
                progress++;
                didProcess = true;
                if (progress >= processTime) {
                    consume(recipe);
                    ItemStack out = recipe.roll(level.random);
                    if (!out.isEmpty()) insertOutput(out);
                    progress = 0;
                }
            }
        }
        dataChanged();
        networkPackMK2(100);
    }

    private boolean canOutput(RockMillRecipe recipe) {
        for (int i = SLOT_OUT_START; i <= 7; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    private void consume(RockMillRecipe recipe) {
        for (AStack key : recipe.inputs) {
            for (int i = SLOT_IN_START; i < SLOT_OUT_START; i++) {
                if (key.matchesRecipe(inventory.getStackInSlot(i), false)) {
                    inventory.extractItem(i, key.count(), false);
                    break;
                }
            }
        }
        if (recipe.fluid != null) inputTank.setFill(inputTank.getFill() - recipe.fluid.fill);
    }

    private void insertOutput(ItemStack out) {
        for (int i = SLOT_OUT_START; i <= 7; i++) {
            ItemStack leftover = inventory.insertItem(i, out, false);
            if (leftover.isEmpty()) return;
            out = leftover;
        }
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 3, worldPosition.getY(), worldPosition.getZ() + 1, Direction.EAST),
                new DirPos(worldPosition.getX() + 3, worldPosition.getY(), worldPosition.getZ() - 1, Direction.EAST),
                new DirPos(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() + 1, Direction.WEST),
                new DirPos(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 1, Direction.WEST),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 3, Direction.SOUTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() + 3, Direction.SOUTH),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() - 3, Direction.NORTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 3, Direction.NORTH),
        };
    }

    public int getProgressScaled(int i) {
        return (progress * i) / Math.max(1, processTime);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(inputTank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(outputTank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(inputTank, outputTank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        inputTank.writeToNBT(tag, "i0");
        outputTank.writeToNBT(tag, "o0");
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputTank.readFromNBT(tag, "i0");
        outputTank.readFromNBT(tag, "o0");
        power = tag.getLong("power");
        maxPower = tag.getLong("maxPower");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        inputTank.serialize(buf);
        outputTank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        buf.writeInt(progress);
        buf.writeInt(processTime);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        inputTank.deserialize(buf);
        outputTank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        progress = buf.readInt();
        processTime = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RockMillMenu(id, inv, this);
    }
}
