package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeTsarMenu;
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
 * Ported from CE's {@code TileEntityNukeTsar} (145 lines, read in full) - 6-slot two-stage casing
 * (same "man" first stage as {@code NukeMike}, {@code tsar_core} added on top for the second stage).
 * CE's own {@code update()}/{@code resizeInventory} pair is a dead migration guard against a save
 * from before the inventory was capped at 6 slots - not ported (nothing in a fresh port ever creates
 * a &gt;6-slot instance to migrate away from).
 */
public class NukeTsarBlockEntity extends NukeCasingBlockEntity {

    public NukeTsarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6);
    }

    public boolean isReady() {
        return inventory.getStackInSlot(0).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(1).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(2).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(3).getItem() == NukeCasingItems.EXPLOSIVE_LENSES.get()
                && inventory.getStackInSlot(4).getItem() == NukeCasingItems.MAN_CORE.get();
    }

    public boolean isFilled() {
        return isReady() && inventory.getStackInSlot(5).getItem() == NukeCasingItems.TSAR_CORE.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeTsar");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeTsarMenu(containerId, playerInventory, this);
    }
}
