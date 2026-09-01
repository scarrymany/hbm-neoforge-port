package com.hbm.inventory.container.machine;

import com.hbm.interfaces.IKeypadHandler;
import com.hbm.inventory.container.network.RadioNetworkMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Functional keypad GUI — CE uses world-space KeypadClient; this is the handler path. */
public class KeypadMenu extends AbstractContainerMenu {

    public final IKeypadHandler handler;
    public final BlockEntity be;

    public KeypadMenu(int id, Inventory playerInv, IKeypadHandler handler) {
        super(RadioNetworkMenus.KEYPAD.get(), id);
        this.handler = handler;
        this.be = handler instanceof BlockEntity te ? te : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null || be.isRemoved()) return false;
        return player.distanceToSqr(
                be.getBlockPos().getX() + 0.5,
                be.getBlockPos().getY() + 0.5,
                be.getBlockPos().getZ() + 0.5) < 64.0;
    }
}
