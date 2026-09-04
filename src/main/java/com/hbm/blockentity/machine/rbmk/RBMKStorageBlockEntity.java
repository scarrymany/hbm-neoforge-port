package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.rbmk.IRBMKLoadable;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.inventory.container.machine.rbmk.RBMKStorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Storage column. Exact CE {@code TileEntityRBMKStorage.java:22-99}: 12 slots, compact toward 0,
 * {@code canLoad} slot 11 empty, load into 11, unload from 0. {@code isItemValidForSlot} always
 * true (CE {@code :65-67}).
 */
public class RBMKStorageBlockEntity extends RBMKSlottedBlockEntity implements IRBMKLoadable, MenuProvider {

    public static final int SLOTS = 12;

    public RBMKStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkStorage");
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            // CE :32-43 — pack occupied slots toward 0
            int freeSlot = 0;
            for (int i = 0; i < 12; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    continue;
                } else {
                    if (inventory.getStackInSlot(freeSlot).isEmpty()) {
                        moveItem(i, freeSlot);
                    }
                    freeSlot++;
                }
            }
        }
        super.updateEntity();
    }

    // CE :48-51
    public void moveItem(int fromSlot, int toSlot) {
        inventory.setStackInSlot(toSlot, inventory.getStackInSlot(fromSlot).copy());
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
        return true;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public boolean canLoad(ItemStack toLoad) {
        return toLoad != null && inventory.getStackInSlot(11).isEmpty();
    }

    @Override
    public void load(ItemStack toLoad) {
        inventory.setStackInSlot(11, toLoad.copy());
        setChanged();
    }

    @Override
    public boolean canUnload() {
        return !inventory.getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack provideNext() {
        return inventory.getStackInSlot(0);
    }

    @Override
    public void unload() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RBMKStorageMenu(containerId, playerInventory, this);
    }
}
