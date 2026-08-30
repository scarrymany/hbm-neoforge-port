package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeBoyMenu;
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
 * Ported from CE's {@code TileEntityNukeBoy} (117 lines, read in full) - a plain 5-slot "gun-type"
 * casing. {@code isReady()} is a flat item-identity chain over all 5 slots (shielding, target,
 * bullet, propellant, igniter), matching CE exactly.
 */
public class NukeBoyBlockEntity extends NukeCasingBlockEntity {

    public NukeBoyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5);
    }

    public boolean isReady() {
        return inventory.getStackInSlot(0).getItem() == NukeCasingItems.BOY_SHIELDING.get()
                && inventory.getStackInSlot(1).getItem() == NukeCasingItems.BOY_TARGET.get()
                && inventory.getStackInSlot(2).getItem() == NukeCasingItems.BOY_BULLET.get()
                && inventory.getStackInSlot(3).getItem() == NukeCasingItems.BOY_PROPELLANT.get()
                && inventory.getStackInSlot(4).getItem() == NukeCasingItems.BOY_IGNITER.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeBoy");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeBoyMenu(containerId, playerInventory, this);
    }
}
