package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionBreederBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotCraftingOutput;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFusionBreeder}: id 26,72 / in 48,45 / out 112,45 / player 8,118. */
public class FusionBreederMenu extends MenuBase<FusionBreederBlockEntity> {

    public FusionBreederMenu(int id, Inventory playerInv, FusionBreederBlockEntity be) {
        super(FusionMenus.FUSION_BREEDER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 72));
        this.addSlot(new SlotNonRetarded(tile, 1, 48, 45));
        this.addSlot(new SlotCraftingOutput(playerInv.player, tile, 2, 112, 45));
        playerInv(playerInv, 8, 118);
    }
}
