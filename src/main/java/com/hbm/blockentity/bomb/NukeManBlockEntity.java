package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeManMenu;
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
 * Ported from CE's {@code TileEntityNukeMan} (133 lines, read in full) - 6-slot Fat Man casing. Slot 0
 * igniter, slots 1-4 explosive lenses (shared item with {@code NukeGadget}), slot 5 core.
 */
public class NukeManBlockEntity extends NukeCasingBlockEntity {

    public NukeManBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6);
    }

    private boolean exp(int slot) {
        return !inventory.getStackInSlot(slot).isEmpty()
                && inventory.getStackInSlot(slot).getItem() == NukeCasingItems.EARLY_EXPLOSIVE_LENSES.get();
    }

    public boolean isReady() {
        if (!(exp(1) && exp(2) && exp(3) && exp(4))) return false;
        return !inventory.getStackInSlot(0).isEmpty() && !inventory.getStackInSlot(5).isEmpty()
                && inventory.getStackInSlot(0).getItem() == NukeCasingItems.MAN_IGNITER.get()
                && inventory.getStackInSlot(5).getItem() == NukeCasingItems.MAN_CORE.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeMan");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeManMenu(containerId, playerInventory, this);
    }
}
