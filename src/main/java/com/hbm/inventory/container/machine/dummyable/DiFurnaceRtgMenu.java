package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceRtgBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerDiFurnaceRTG.java:29-48}: in 80,18+54 / out 134,36 /
 * pellets 22/40 × 18/36/54; playerInv 8,84 / 142.
 */
public class DiFurnaceRtgMenu extends MenuBase<MachineDiFurnaceRtgBlockEntity> {

    public DiFurnaceRtgMenu(int id, Inventory playerInv, MachineDiFurnaceRtgBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_DIFURNACE_RTG.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 54));
        this.addSlot(new SlotTakeOnly(tile, 2, 134, 36));
        this.addSlots(tile, 3, 22, 18, 3, 2);
        playerInv(playerInv, 8, 84, 142);
    }
}
