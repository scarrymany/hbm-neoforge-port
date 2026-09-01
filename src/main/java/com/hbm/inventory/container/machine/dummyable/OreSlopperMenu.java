package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineOreSlopperBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerOreSlopper}: bat 8,72 / ID 26,72 / in 71,27 / out 134-152 × 18/36/54 / upgrades 62,72 + 80,72. */
public class OreSlopperMenu extends MenuBase<MachineOreSlopperBlockEntity> {

    public OreSlopperMenu(int id, Inventory playerInv, MachineOreSlopperBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ORE_SLOPPER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 72));
        this.addSlot(new SlotNonRetarded(tile, 1, 26, 72));
        this.addSlot(new SlotNonRetarded(tile, 2, 71, 27));
        this.addSlot(new SlotTakeOnly(tile, 3, 134, 18));
        this.addSlot(new SlotTakeOnly(tile, 4, 152, 18));
        this.addSlot(new SlotTakeOnly(tile, 5, 134, 36));
        this.addSlot(new SlotTakeOnly(tile, 6, 152, 36));
        this.addSlot(new SlotTakeOnly(tile, 7, 134, 54));
        this.addSlot(new SlotTakeOnly(tile, 8, 152, 54));
        this.addSlot(new SlotNonRetarded(tile, 9, 62, 72));
        this.addSlot(new SlotNonRetarded(tile, 10, 80, 72));
        playerInv(playerInv, 8, 122, 180);
    }
}
