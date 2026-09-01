package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineExposureChamberBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineExposureChamber}: particle 8,18 / container 8,54 / ingredient 80,36 / out 116,36 / bat 152,54 / upgrades 44,54 + 62,54. */
public class ExposureChamberMenu extends MenuBase<MachineExposureChamberBlockEntity> {

    public ExposureChamberMenu(int id, Inventory playerInv, MachineExposureChamberBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_EXPOSURE_CHAMBER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 18));
        this.addSlot(new SlotTakeOnly(tile, 1, -2000, -2000));
        this.addSlot(new SlotTakeOnly(tile, 2, 8, 54));
        this.addSlot(new SlotNonRetarded(tile, 3, 80, 36));
        this.addSlot(new SlotTakeOnly(tile, 4, 116, 36));
        this.addSlot(new SlotNonRetarded(tile, 5, 152, 54));
        this.addSlot(new SlotNonRetarded(tile, 6, 44, 54));
        this.addSlot(new SlotNonRetarded(tile, 7, 62, 54));
        playerInv(playerInv, 8, 104, 162);
    }
}
