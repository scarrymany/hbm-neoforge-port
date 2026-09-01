package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.RadarScreenBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE radar screen is overlay-only. Inspect + unused slot. */
public class RadarScreenMenu extends MenuBase<RadarScreenBlockEntity> {

    public RadarScreenMenu(int id, Inventory playerInv, RadarScreenBlockEntity be) {
        super(DummyableProcessMenus.RADAR_SCREEN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
