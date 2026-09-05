package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineRadarBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineRadarNT} — slots 0–7 sat_relay, 8 linker, 9 battery. Map GUI skipped. */
public class RadarMenu extends MenuBase<MachineRadarBlockEntity> {

    public RadarMenu(int id, Inventory playerInv, MachineRadarBlockEntity be) {
        super(SensorMenus.MACHINE_RADAR.get(), id, be);
        for (int i = 0; i < 8; i++) {
            this.addSlot(new SlotNonRetarded(tile, i, 26 + i * 18, 50));
        }
        this.addSlot(new SlotNonRetarded(tile, MachineRadarBlockEntity.LINKER_SLOT, 26, 108));
        this.addSlot(new SlotNonRetarded(tile, MachineRadarBlockEntity.BATTERY_SLOT, 8, 108));
        playerInv(playerInv, 8, 130);
    }
}
