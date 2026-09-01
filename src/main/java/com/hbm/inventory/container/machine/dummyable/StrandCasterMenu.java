package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineStrandCasterBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineStrandCaster}: mold 57,62 / output 2×3 / player 132+190. */
public class StrandCasterMenu extends MenuBase<MachineStrandCasterBlockEntity> {

    public StrandCasterMenu(int id, Inventory playerInv, MachineStrandCasterBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_STRAND_CASTER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 57, 62));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                this.addSlot(new SlotTakeOnly(tile, j + i * 2 + 1, 125 + j * 18, 26 + i * 18));
            }
        }
        playerInv(playerInv, 8, 132, 190);
    }
}
