package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineHydrotreaterBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineHydrotreater} 9-slot layout (H₂ canister slots already gone in CE). */
public class HydrotreaterMenu extends MenuBase<MachineHydrotreaterBlockEntity> {

    public HydrotreaterMenu(int id, Inventory playerInv, MachineHydrotreaterBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_HYDROTREATER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 90));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 90));
        this.addSlot(new SlotTakeOnly(tile, 2, 35, 108));
        this.addSlot(new SlotNonRetarded(tile, 3, 125, 90));
        this.addSlot(new SlotTakeOnly(tile, 4, 125, 108));
        this.addSlot(new SlotNonRetarded(tile, 5, 143, 90));
        this.addSlot(new SlotTakeOnly(tile, 6, 143, 108));
        this.addSlot(new SlotNonRetarded(tile, 7, 17, 108));
        this.addSlot(new SlotNonRetarded(tile, 8, 89, 36));
        playerInv(playerInv, 8, 156, 214);
    }
}
