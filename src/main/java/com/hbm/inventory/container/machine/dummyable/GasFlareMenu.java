package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineGasFlareBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerMachineGasFlare} — battery / canister / ID / upgrades. Valve+burn buttons. */
public class GasFlareMenu extends MenuBase<MachineGasFlareBlockEntity> {

    public static final int BUTTON_VALVE = 0;
    public static final int BUTTON_BURN = 1;

    public GasFlareMenu(int id, Inventory playerInv, MachineGasFlareBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_FLARE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 143, 71));
        this.addSlot(new SlotNonRetarded(tile, 1, 17, 17));
        this.addSlot(new SlotTakeOnly(tile, 2, 17, 53));
        this.addSlot(new SlotNonRetarded(tile, 3, 35, 71));
        this.addSlot(new SlotNonRetarded(tile, 4, 80, 71));
        this.addSlot(new SlotNonRetarded(tile, 5, 98, 71));
        playerInv(playerInv, 8, 121, 179);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_VALVE) {
            be.toggleValve();
            return true;
        }
        if (id == BUTTON_BURN) {
            be.toggleBurn();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
