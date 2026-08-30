package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.IcfReactorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported from CE's {@code ContainerICF}: 5 fresh-pellet input, 1 active/reacting, 5 spent output. */
public class IcfReactorMenu extends MenuBase<IcfReactorBlockEntity> {

    public IcfReactorMenu(int id, Inventory playerInv, IcfReactorBlockEntity be) {
        super(FusionMenus.ICF_REACTOR.get(), id, be);

        for (int i = 0; i < 5; i++) this.addSlot(new SlotNonRetarded(tile, i, 80 + i * 18, 18));
        this.addSlot(new SlotNonRetarded(tile, 5, 116, 54));
        for (int i = 0; i < 5; i++) this.addSlot(new SlotTakeOnly(tile, 6 + i, 80 + i * 18, 90));

        playerInv(playerInv, 44, 140);
    }
}
