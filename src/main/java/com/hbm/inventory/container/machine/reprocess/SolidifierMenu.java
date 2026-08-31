package com.hbm.inventory.container.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.SolidifierBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerSolidifier}: output + battery. Upgrades not ported. */
public class SolidifierMenu extends MenuBase<SolidifierBlockEntity> {

    public SolidifierMenu(int id, Inventory playerInv, SolidifierBlockEntity be) {
        super(ReprocessMenus.MACHINE_SOLIDIFIER.get(), id, be);

        this.addSlot(new SlotTakeOnly(tile, 0, 80, 39));
        this.addSlot(new SlotNonRetarded(tile, 1, 8, 21));

        playerInv(playerInv, 8, 104);
    }
}
