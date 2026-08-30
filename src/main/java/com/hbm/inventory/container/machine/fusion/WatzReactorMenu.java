package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.WatzReactorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Ported from CE's {@code ContainerWatz}: 24 pellet slots laid out in CE's own diamond-cut 6x6 grid. */
public class WatzReactorMenu extends MenuBase<WatzReactorBlockEntity> {

    public WatzReactorMenu(int id, Inventory playerInv, WatzReactorBlockEntity be) {
        super(FusionMenus.WATZ_REACTOR.get(), id, be);

        int index = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                if (i + j > 1 && i + j < 9 && 5 - i + j > 1 && i + 5 - j > 1) {
                    this.addSlot(new SlotNonRetarded(tile, index, 17 + i * 18, 8 + j * 18));
                    index++;
                }
            }
        }

        playerInv(playerInv, 8, 147);
    }
}
