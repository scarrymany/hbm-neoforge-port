package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterHeatexBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerHeaterHeatex.java:24-34}: ID 80,72; playerInv 8,122 / hotbar 180.
 * Invented clickMenuButton +/- handlers removed — GUI uses {@code NBTControlPacket} like CE.
 */
public class HeaterHeatexMenu extends MenuBase<HeaterHeatexBlockEntity> {

    public HeaterHeatexMenu(int id, Inventory playerInv, HeaterHeatexBlockEntity be) {
        super(DummyableProcessMenus.HEATER_HEATEX.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 72));
        playerInv(playerInv, 8, 122);
    }
}
