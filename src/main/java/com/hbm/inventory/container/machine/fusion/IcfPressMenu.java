package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.IcfPressBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code ContainerICFPress.java:35-48}: pellet/muon/fuel + ID 62,18 / 134,18. */
public class IcfPressMenu extends MenuBase<IcfPressBlockEntity> {

    public IcfPressMenu(int id, Inventory playerInv, IcfPressBlockEntity be) {
        super(FusionMenus.ICF_PRESS.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 98, 18));
        this.addSlot(new SlotTakeOnly(tile, 1, 98, 54));
        this.addSlot(new SlotNonRetarded(tile, 2, 8, 18));
        this.addSlot(new SlotTakeOnly(tile, 3, 8, 54));
        this.addSlot(new SlotNonRetarded(tile, 4, 62, 54));
        this.addSlot(new SlotNonRetarded(tile, 5, 134, 54));
        this.addSlot(new SlotNonRetarded(tile, 6, 62, 18));
        this.addSlot(new SlotNonRetarded(tile, 7, 134, 18));

        playerInv(playerInv, 8, 97);
    }
}
