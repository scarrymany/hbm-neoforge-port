package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.rbmk.IRBMKLoadable;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spent/spare fuel rod storage column - a 9-slot buffer an autoloader or crane can pull from/push
 * into. Ported from CE's {@code TileEntityRBMKStorage} (111 lines).
 */
public class RBMKStorageBlockEntity extends RBMKSlottedBlockEntity implements IRBMKLoadable {

    public static final int SLOTS = 9;

    public RBMKStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkStorage");
    }

    public void moveItem(int fromSlot, int toSlot) {
        ItemStack from = inventory.getStackInSlot(fromSlot);
        if (from.isEmpty() || !inventory.getStackInSlot(toSlot).isEmpty()) return;
        inventory.setStackInSlot(toSlot, from);
        inventory.setStackInSlot(fromSlot, ItemStack.EMPTY);
    }

    @Override
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.OTHER;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.STORAGE;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return stack.getItem() instanceof com.hbm.items.machine.ItemRBMKRod;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public boolean canLoad(ItemStack toLoad) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void load(ItemStack toLoad) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, toLoad.copy());
                setChanged();
                return;
            }
        }
    }

    @Override
    public boolean canUnload() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public ItemStack provideNext() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void unload() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }
    }
}
