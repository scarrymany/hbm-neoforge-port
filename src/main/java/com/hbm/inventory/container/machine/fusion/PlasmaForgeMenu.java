package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.PlasmaForgeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

public class PlasmaForgeMenu extends MenuBase<PlasmaForgeBlockEntity> {

    public PlasmaForgeMenu(int id, Inventory playerInv, PlasmaForgeBlockEntity be) {
        super(FusionMenus.FUSION_PLASMA_FORGE.get(), id, be);
        for (int i = 0; i < 6; i++) {
            this.addSlot(new SlotNonRetarded(tile, i, 26 + (i % 3) * 18, 18 + (i / 3) * 18));
        }
        this.addSlot(new SlotTakeOnly(tile, 6, 134, 27));
        this.addSlot(new SlotNonRetarded(tile, 7, 8, 54));
        playerInv(playerInv, 8, 104);
    }
}
