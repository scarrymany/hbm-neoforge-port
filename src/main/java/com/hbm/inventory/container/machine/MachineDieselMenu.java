package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineDieselBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Ported (slot layout trimmed, see {@link MachineDieselBlockEntity}'s javadoc) from CE's
 * {@code ContainerMachineDiesel}: only the battery-charging slot survives, kept at CE's own pixel
 * position. Button id 0 is the on/off toggle, wired through vanilla's own
 * {@link net.minecraft.world.inventory.AbstractContainerMenu#clickMenuButton} plumbing (the same
 * mechanism vanilla's loom/stonecutter/enchanting-table buttons use) rather than a bespoke packet.
 */
public class MachineDieselMenu extends MenuBase<MachineDieselBlockEntity> {

    public static final int BUTTON_TOGGLE_ON = 0;

    public MachineDieselMenu(int id, Inventory playerInv, MachineDieselBlockEntity be) {
        super(PowerGenMenus.MACHINE_DIESEL.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 141, 71));

        playerInv(playerInv, 8, 121, 179);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_ON) {
            be.setOn(!be.isOn);
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
