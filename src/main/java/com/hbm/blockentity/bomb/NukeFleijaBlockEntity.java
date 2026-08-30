package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeFleijaMenu;
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
 * Ported from CE's {@code TileEntityNukeFleija} (122 lines, read in full) - 11-slot F.L.E.I.J.A.
 * casing: slots 0-1 igniter, 2-4 propellant, 5-10 (six) core.
 */
public class NukeFleijaBlockEntity extends NukeCasingBlockEntity {

    public NukeFleijaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11);
    }

    public boolean isReady() {
        return inventory.getStackInSlot(0).getItem() == NukeCasingItems.FLEIJA_IGNITER.get()
                && inventory.getStackInSlot(1).getItem() == NukeCasingItems.FLEIJA_IGNITER.get()
                && inventory.getStackInSlot(2).getItem() == NukeCasingItems.FLEIJA_PROPELLANT.get()
                && inventory.getStackInSlot(3).getItem() == NukeCasingItems.FLEIJA_PROPELLANT.get()
                && inventory.getStackInSlot(4).getItem() == NukeCasingItems.FLEIJA_PROPELLANT.get()
                && inventory.getStackInSlot(5).getItem() == NukeCasingItems.FLEIJA_CORE.get()
                && inventory.getStackInSlot(6).getItem() == NukeCasingItems.FLEIJA_CORE.get()
                && inventory.getStackInSlot(7).getItem() == NukeCasingItems.FLEIJA_CORE.get()
                && inventory.getStackInSlot(8).getItem() == NukeCasingItems.FLEIJA_CORE.get()
                && inventory.getStackInSlot(9).getItem() == NukeCasingItems.FLEIJA_CORE.get()
                && inventory.getStackInSlot(10).getItem() == NukeCasingItems.FLEIJA_CORE.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeFleija");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeFleijaMenu(containerId, playerInventory, this);
    }
}
