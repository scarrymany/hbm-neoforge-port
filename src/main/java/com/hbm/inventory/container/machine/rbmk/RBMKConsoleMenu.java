package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Console menu. Exact CE {@code provideContainer} returns null — no slots.
 */
public class RBMKConsoleMenu extends MenuBase<RBMKConsoleBlockEntity> {

    public RBMKConsoleMenu(int id, Inventory playerInventory, RBMKConsoleBlockEntity be) {
        super(RBMKMenuTypes.CONSOLE.get(), id, be);
    }

    public static RBMKConsoleMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKConsoleBlockEntity be) {
            return new RBMKConsoleMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKConsoleBlockEntity at " + pos);
    }
}
