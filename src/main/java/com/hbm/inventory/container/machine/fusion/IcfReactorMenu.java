package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.IcfReactorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerICF.java:20-23}: 5 fresh-pellet input, 1 active/reacting, 5 spent output,
 * coolant ID 44,90. {@code setType(11)} Exact CE {@code TileEntityICF.java:82}.
 */
public class IcfReactorMenu extends MenuBase<IcfReactorBlockEntity> {

    public IcfReactorMenu(int id, Inventory playerInv, IcfReactorBlockEntity be) {
        super(FusionMenus.ICF_REACTOR.get(), id, be);

        for (int i = 0; i < 5; i++) this.addSlot(new SlotNonRetarded(tile, i, 80 + i * 18, 18));
        this.addSlot(new SlotNonRetarded(tile, 5, 116, 54));
        for (int i = 0; i < 5; i++) this.addSlot(new SlotTakeOnly(tile, 6 + i, 80 + i * 18, 90));
        this.addSlot(new SlotNonRetarded(tile, 11, 44, 90));

        playerInv(playerInv, 44, 140);
    }
}
