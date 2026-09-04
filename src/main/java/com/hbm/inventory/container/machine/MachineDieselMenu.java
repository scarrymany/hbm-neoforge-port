package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineDieselBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Exact CE {@code ContainerMachineDiesel.java:38-41}: canister 17,17 / empty 17,53 / battery 141,71 / ID 35,71.
 * {@code setType(3)} / {@code loadTank(0,1)} Exact CE {@code TileEntityMachineDiesel.java:120-121}.
 */
public class MachineDieselMenu extends MenuBase<MachineDieselBlockEntity> {

    public static final int BUTTON_TOGGLE_ON = 0;

    public MachineDieselMenu(int id, Inventory playerInv, MachineDieselBlockEntity be) {
        super(PowerGenMenus.MACHINE_DIESEL.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 17, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 17, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 141, 71));
        this.addSlot(new SlotNonRetarded(tile, 3, 35, 71));

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
