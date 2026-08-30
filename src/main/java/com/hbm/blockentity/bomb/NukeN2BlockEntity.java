package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeN2Menu;
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
 * Ported from CE's {@code TileEntityNukeN2} (136 lines, read in full) - 12-slot "mine" casing, each
 * slot capped to a single item (CE's {@code getStackLimit}/{@code getSlotLimit} both return 1); yield
 * scales with how many of the 12 slots hold an {@code n2_charge}.
 */
public class NukeN2BlockEntity extends NukeCasingBlockEntity {

    public NukeN2BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 12, 1);
    }

    public int countCharges() {
        int charges = 0;
        for (int i = 0; i < 12; i++) {
            if (inventory.getStackInSlot(i).getItem() == NukeCasingItems.N2_CHARGE.get()) charges++;
        }
        return charges;
    }

    public boolean isReady() {
        return countCharges() > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeN2");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeN2Menu(containerId, playerInventory, this);
    }
}
