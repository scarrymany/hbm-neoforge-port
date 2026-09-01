package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFusionKlystron}: battery 8,72 / player 17,118. */
public class FusionKlystronMenu extends MenuBase<FusionKlystronBlockEntity> {

    public FusionKlystronMenu(int id, Inventory playerInv, FusionKlystronBlockEntity be) {
        super(FusionMenus.FUSION_KLYSTRON.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 72));
        playerInv(playerInv, 17, 118);
    }
}
