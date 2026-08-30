package com.hbm.blockentity.machine.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Inventoried RBMK column base, ported from CE's {@code TileEntityRBMKSlottedBase} (extends
 * {@code TileEntityRBMKActiveBase}, which itself only added {@code isUseableByPlayer}/abstract
 * {@code getName} on top of {@code TileEntityRBMKBase} - both folded into this one class rather than
 * preserved as a separate trivial intermediate, since nothing outside CE's own RBMK package ever
 * named {@code TileEntityRBMKActiveBase} directly).
 * <p>
 * <b>Why this duplicates {@code MachineBaseBlockEntity}'s inventory/capability shape instead of
 * extending it</b>: every RBMK column (this class's subclasses) needs the heat/meltdown/lid/console
 * machinery on {@link RBMKBaseBlockEntity} (forward reference - the parallel {@code rbmk_core_logic}
 * package's port of CE's {@code TileEntityRBMKBase}, expected to extend
 * {@code com.hbm.blockentity.LoadedBaseBlockEntity}), and Java single inheritance means a class can
 * extend only one of {@code RBMKBaseBlockEntity} or {@code MachineBaseBlockEntity}. CE resolved the
 * identical fork the same way (its own {@code RBMKSlottedItemStackHandler} inner class, entirely
 * separate from {@code TileEntityMachineBase}) - this class mirrors that shape against this port's
 * capability-accessor-method convention (see {@code MachineBaseBlockEntity}'s own javadoc on why
 * NeoForge 1.21.1 exposes capabilities that way) instead of CE's Forge-1.12
 * {@code getCapability}/{@code hasCapability} override pair.
 */
public abstract class RBMKSlottedBlockEntity extends RBMKBaseBlockEntity {

    public final ItemStackHandler inventory;
    private IItemHandlerModifiable checkedInventory;

    protected RBMKSlottedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.inventory = new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }
        };
    }

    protected abstract Component getDefaultName();

    /** CE: {@code TileEntityRBMKActiveBase.isUseableByPlayer(EntityPlayer)}, folded in - see class javadoc. */
    public boolean isUseableByPlayer(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    public boolean canInsertItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    public IItemHandlerModifiable getCheckedInventory() {
        if (checkedInventory == null) checkedInventory = new CheckedInventory();
        return checkedInventory;
    }

    @Nullable
    public IItemHandlerModifiable getItemHandlerCapability(@Nullable Direction side) {
        return inventory;
    }

    public boolean hasItemHandlerCapability(@Nullable Direction side) {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    private final class CheckedInventory implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValidForSlot(slot, stack)) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !canExtractItem(slot, inventory.getStackInSlot(slot), amount)) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isItemValidForSlot(slot, stack);
        }
    }
}
