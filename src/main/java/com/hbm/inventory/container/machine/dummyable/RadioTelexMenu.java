package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.RadioTelexBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE telex is a screen-only GUI. Paper slot + channel inspect. */
public class RadioTelexMenu extends MenuBase<RadioTelexBlockEntity> {

    public RadioTelexMenu(int id, Inventory playerInv, RadioTelexBlockEntity be) {
        super(DummyableProcessMenus.RADIO_TELEX.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
