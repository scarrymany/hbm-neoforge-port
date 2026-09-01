package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFusionTorus}: battery 8,82 / blueprints 71,81 / out 130,36 / player 35,162. */
public class FusionTorusMenu extends MenuBase<FusionTorusBlockEntity> {

    public FusionTorusMenu(int id, Inventory playerInv, FusionTorusBlockEntity be) {
        super(FusionMenus.FUSION_TORUS.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 82));
        this.addSlot(new SlotNonRetarded(tile, 1, 71, 81));
        this.addSlot(new SlotTakeOnly(tile, 2, 130, 36));
        playerInv(playerInv, 35, 162);
    }
}
