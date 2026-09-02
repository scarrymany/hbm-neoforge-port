package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeMikeMenu;
import com.hbm.items.bomb.NukeCasingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Ported from CE's {@code TileEntityNukeMike} (131 lines, read in full) - 8-slot two-stage casing.
 * Slots 0-3 explosive lenses + slot 4 core form the "man-equivalent" first stage
 * ({@link #isReady()}); adding slot 5 core, slot 6 deuterium, slot 7 cooling unit on top completes
 * the "mike" second stage ({@link #isFilled()}).
 */
public class NukeMikeBlockEntity extends NukeCasingBlockEntity {

    public NukeMikeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8);
    }

    public boolean isReady() {
        return inventory.getStackInSlot(0).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(1).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(2).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(3).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(4).getItem() == com.hbm.items.special.SpecialItems.MAN_CORE.get();
    }

    public boolean isFilled() {
        return isReady()
                && inventory.getStackInSlot(5).getItem() == NukeCasingItems.MIKE_CORE.get()
                && inventory.getStackInSlot(6).getItem() == NukeCasingItems.MIKE_DEUT.get()
                && inventory.getStackInSlot(7).getItem() == NukeCasingItems.MIKE_COOLING_UNIT.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeMike");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeMikeMenu(containerId, playerInventory, this);
    }
}
