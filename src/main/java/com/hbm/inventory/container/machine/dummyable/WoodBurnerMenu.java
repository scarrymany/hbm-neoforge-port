package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineWoodBurnerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineWoodBurner.java:34-53}: fuel 26,18 / ash 26,54 / ID 98,54 /
 * fluid 98,18+36 / bat 143,54; playerInv 8,104 / 162.
 * Invented clickMenuButton handlers removed — GUI uses {@code NBTControlPacket}.
 */
public class WoodBurnerMenu extends MenuBase<MachineWoodBurnerBlockEntity> {

    public WoodBurnerMenu(int id, Inventory playerInv, MachineWoodBurnerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_WOOD_BURNER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 18));
        this.addSlot(new SlotTakeOnly(tile, 1, 26, 54));
        this.addSlot(new SlotNonRetarded(tile, 2, 98, 54));
        this.addSlot(new SlotNonRetarded(tile, 3, 98, 18));
        this.addSlot(new SlotTakeOnly(tile, 4, 98, 36));
        this.addSlot(new SlotNonRetarded(tile, 5, 143, 54));
        playerInv(playerInv, 8, 104, 162);
    }
}
