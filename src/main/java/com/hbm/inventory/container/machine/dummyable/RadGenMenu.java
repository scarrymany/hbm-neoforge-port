package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRadGenBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineRadGen}: 3×4 in at 8,17; 3×4 out at 116,17. */
public class RadGenMenu extends MenuBase<MachineRadGenBlockEntity> {

    public RadGenMenu(int id, Inventory playerInv, MachineRadGenBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_RADGEN.get(), id, be);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotNonRetarded(tile, j + i * 3, 8 + j * 18, 17 + i * 18));
            }
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotTakeOnly(tile, j + i * 3 + 12, 116 + j * 18, 17 + i * 18));
            }
        }
        playerInv(playerInv, 8, 102, 160);
    }
}
