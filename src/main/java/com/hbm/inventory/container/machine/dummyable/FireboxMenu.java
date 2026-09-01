package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterFireboxBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFirebox}: fuel 26+44,36. */
public class FireboxMenu extends MenuBase<HeaterFireboxBlockEntity> {

    public FireboxMenu(int id, Inventory playerInv, HeaterFireboxBlockEntity be) {
        super(DummyableProcessMenus.HEATER_FIREBOX.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 36));
        this.addSlot(new SlotNonRetarded(tile, 1, 44, 36));
        playerInv(playerInv, 8, 86);
    }
}
