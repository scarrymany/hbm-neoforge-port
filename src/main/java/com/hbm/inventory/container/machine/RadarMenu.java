package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineRadarBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineRadarNT} — battery slot only this pass (map GUI not ported). */
public class RadarMenu extends MenuBase<MachineRadarBlockEntity> {

    public RadarMenu(int id, Inventory playerInv, MachineRadarBlockEntity be) {
        super(SensorMenus.MACHINE_RADAR.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, MachineRadarBlockEntity.BATTERY_SLOT, 8, 108));
        playerInv(playerInv, 8, 130);
    }
}
