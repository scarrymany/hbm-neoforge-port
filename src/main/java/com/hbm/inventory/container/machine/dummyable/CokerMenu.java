package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCokerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotCraftingOutput;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineCoker} slots 35,72 / 97,27. */
public class CokerMenu extends MenuBase<MachineCokerBlockEntity> {

    public CokerMenu(int id, Inventory playerInv, MachineCokerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_COKER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 35, 72));
        this.addSlot(new SlotCraftingOutput(playerInv.player, tile, 1, 97, 27));
        playerInv(playerInv, 8, 122, 180);
    }
}
