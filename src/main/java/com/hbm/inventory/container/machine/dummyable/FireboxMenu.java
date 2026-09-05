package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterFireboxBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code ContainerFirebox.java:25-36}: fuel 44,27 + 62,27; playerInv 8,86 / hotbar 144. */
public class FireboxMenu extends MenuBase<HeaterFireboxBlockEntity> {

    public FireboxMenu(int id, Inventory playerInv, HeaterFireboxBlockEntity be) {
        super(DummyableProcessMenus.HEATER_FIREBOX.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 27));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 27));
        playerInv(playerInv, 8, 86);
    }
}
