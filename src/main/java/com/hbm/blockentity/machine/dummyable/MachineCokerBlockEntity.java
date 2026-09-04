package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CokerMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.util.Tuple.Triplet;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineCoker}: heat-driven, 20k TU/cycle, 100k heat, ΔT×0.25 from below.
 * {@code tanks[0].setType(0)} Exact CE {@code :75}.
 */
public class MachineCokerBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.25D;

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public boolean wasOn;
    public int progress;
    public int heat;

    public MachineCokerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, false);
        this.input = new FluidTankNTM(Fluids.HEAVYOIL, 16_000).withOwner(this);
        this.output = new FluidTankNTM(Fluids.OIL_COKER, 8_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCoker");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 1;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        tryPullHeat();
        // CE TileEntityMachineCoker.java:75
        this.input.setType(0, inventory);

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(input.getTankType(), level, pos);
            }
        }

        wasOn = false;
        if (canProcess()) {
            int burn = heat / 100;
            if (burn > 0) {
                wasOn = true;
                progress += burn;
                heat -= burn;
                if (progress >= PROCESS_TIME) {
                    setChanged();
                    progress -= PROCESS_TIME;
                    Triplet<Integer, ItemStack, FluidStack> recipe = CokerRecipes.getOutput(input.getTankType());
                    int fillReq = recipe.getX();
                    ItemStack itemOut = recipe.getY();
                    FluidStack byproduct = recipe.getZ();
                    if (!itemOut.isEmpty()) {
                        ItemStack slot = inventory.getStackInSlot(1);
                        if (slot.isEmpty()) {
                            inventory.setStackInSlot(1, itemOut.copy());
                        } else {
                            slot.grow(itemOut.getCount());
                        }
                    }
                    if (byproduct != null) {
                        output.setFill(output.getFill() + byproduct.fill);
                    }
                    input.setFill(input.getFill() - fillReq);
                }
            }
        }

        for (DirPos pos : getConPos()) {
            if (output.getFill() > 0) tryProvide(output, level, pos);
        }

        dataChanged();
        networkPackMK2(25);
    }

    public boolean canProcess() {
        Triplet<Integer, ItemStack, FluidStack> recipe = CokerRecipes.getOutput(input.getTankType());
        if (recipe == null) return false;
        int fillReq = recipe.getX();
        ItemStack itemOut = recipe.getY();
        FluidStack byproduct = recipe.getZ();
        if (byproduct != null) output.setTankType(byproduct.type);
        if (input.getFill() < fillReq) return false;
        if (byproduct != null && byproduct.fill + output.getFill() > output.getMaxFill()) return false;
        if (!itemOut.isEmpty() && !inventory.getStackInSlot(1).isEmpty()) {
            ItemStack slot = inventory.getStackInSlot(1);
            if (!ItemStack.isSameItemSameComponents(itemOut, slot)) return false;
            return slot.getCount() + itemOut.getCount() <= slot.getMaxStackSize();
        }
        return true;
    }

    private void tryPullHeat() {
        if (heat >= MAX_HEAT) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(diff);
                heat = Math.min(heat + diff, MAX_HEAT);
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() + 1, Direction.EAST),
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() - 1, Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() + 1, Direction.WEST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 1, Direction.WEST),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
        };
    }

    public int getProgressScaled(int i) {
        return (progress * i) / PROCESS_TIME;
    }

    public int getHeatScaled(int i) {
        return (heat * i) / MAX_HEAT;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        input.writeToNBT(tag, "t0");
        output.writeToNBT(tag, "t1");
        tag.putInt("prog", progress);
        tag.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.readFromNBT(tag, "t0");
        output.readFromNBT(tag, "t1");
        progress = tag.getInt("prog");
        heat = tag.getInt("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(wasOn);
        buf.writeInt(heat);
        buf.writeInt(progress);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        wasOn = buf.readBoolean();
        heat = buf.readInt();
        progress = buf.readInt();
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CokerMenu(id, inv, this);
    }
}
