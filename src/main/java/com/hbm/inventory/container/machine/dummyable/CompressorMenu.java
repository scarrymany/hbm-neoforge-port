package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCompressorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerCompressor.java:32-47}: ID 17,72 / battery 152,72 / upgrades 52,72 + 70,72;
 * playerInv 8,122 / 180. Invented clickMenuButton PU handlers removed.
 */
public class CompressorMenu extends MenuBase<MachineCompressorBlockEntity> {

    public CompressorMenu(int id, Inventory playerInv, MachineCompressorBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_COMPRESSOR.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 72));
        this.addSlot(new SlotNonRetarded(tile, 1, 152, 72));
        this.addSlot(new SlotNonRetarded(tile, 2, 52, 72));
        this.addSlot(new SlotNonRetarded(tile, 3, 70, 72));
        playerInv(playerInv, 8, 122, 180);
    }
}
