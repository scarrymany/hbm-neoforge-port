package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeGadgetMenu;
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
 * Ported from CE's {@code TileEntityNukeGadget} (137 lines, read in full) - 6-slot implosion-type
 * casing. Slot 0 wireing, slots 1-4 the 4 explosive-lens charges (shared item with {@code NukeMan}),
 * slot 5 the core.
 */
public class NukeGadgetBlockEntity extends NukeCasingBlockEntity {

    public NukeGadgetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6);
    }

    private boolean exp(int slot) {
        return inventory.getStackInSlot(slot).getItem() == NukeCasingItems.EARLY_EXPLOSIVE_LENSES.get();
    }

    public boolean isReady() {
        if (!(exp(1) && exp(2) && exp(3) && exp(4))) return false;
        return inventory.getStackInSlot(0).getItem() == NukeCasingItems.GADGET_WIREING.get()
                && inventory.getStackInSlot(5).getItem() == NukeCasingItems.GADGET_CORE.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeGadget");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeGadgetMenu(containerId, playerInventory, this);
    }
}
