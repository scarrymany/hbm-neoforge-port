package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAnnihilatorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineAnnihilator}: in 17,45 / fluid-id 35,45 / out 80,36 2×3 / monitor+payout. */
public class AnnihilatorMenu extends MenuBase<MachineAnnihilatorBlockEntity> {

    public AnnihilatorMenu(int id, Inventory playerInv, MachineAnnihilatorBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ANNIHILATOR.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 45));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 45));
        addOutputSlots(playerInv.player, tile, 2, 80, 36, 2, 3);
        this.addSlot(new SlotNonRetarded(tile, 8, 152, 18));
        this.addSlot(new SlotNonRetarded(tile, 9, 152, 62));
        this.addSlot(new SlotTakeOnly(tile, 10, 152, 80));
        playerInv(playerInv, 8, 126);
    }
}
