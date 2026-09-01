package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRadiolysisBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerRadiolysis} — 10 RTG + fluid ID + sterilize + battery. */
public class RadiolysisMenu extends MenuBase<MachineRadiolysisBlockEntity> {

    public RadiolysisMenu(int id, Inventory playerInv, MachineRadiolysisBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_RADIOLYSIS.get(), id, be);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                this.addSlot(new SlotNonRetarded(tile, j + i * 5, 188 + i * 18, 8 + j * 18));
            }
        }
        this.addSlot(new SlotNonRetarded(tile, 10, 34, 17));
        this.addSlot(new SlotTakeOnly(tile, 11, 34, 53));
        this.addSlot(new SlotNonRetarded(tile, 12, 148, 17));
        this.addSlot(new SlotTakeOnly(tile, 13, 148, 53));
        this.addSlot(new SlotNonRetarded(tile, 14, 8, 53));
        playerInv(playerInv, 8, 84, 142);
    }
}
