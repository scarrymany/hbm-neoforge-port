package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerOilburner.java:26-42}: in 26,17 / out 26,53 / ID 44,71;
 * playerInv 8,121 / hotbar 179. Invented clickMenuButton handlers removed.
 */
public class OilburnerMenu extends MenuBase<HeaterOilburnerBlockEntity> {

    public OilburnerMenu(int id, Inventory playerInv, HeaterOilburnerBlockEntity be) {
        super(DummyableProcessMenus.HEATER_OILBURNER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 26, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 44, 71));
        playerInv(playerInv, 8, 121, 179);
    }
}
