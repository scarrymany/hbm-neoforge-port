package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineChemicalFactoryBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code ContainerMachineChemicalFactory}: battery 224,88; upgrades 206,125/143/161;
 * 4 rows template + 3 in + 3 out @ 16px; playerInv 26,134.
 */
public class MachineChemicalFactoryMenu extends MenuBase<MachineChemicalFactoryBlockEntity> {

    public MachineChemicalFactoryMenu(int id, Inventory playerInv, MachineChemicalFactoryBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_CHEMICAL_FACTORY.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 224, 88));
        this.addSlot(new SlotNonRetarded(tile, 1, 206, 125));
        this.addSlot(new SlotNonRetarded(tile, 2, 206, 143));
        this.addSlot(new SlotNonRetarded(tile, 3, 206, 161));
        for (int i = 0; i < 4; i++) {
            addSlots(tile, MachineChemicalFactoryBlockEntity.blueprintSlot(i), 93, 20 + i * 22, 1, 1, 16);
            addSlots(tile, MachineChemicalFactoryBlockEntity.itemIn(i, 0), 10, 20 + i * 22, 1, 3, 16);
            addOutputSlots(playerInv.player, tile, MachineChemicalFactoryBlockEntity.itemOut(i, 0), 139, 20 + i * 22, 1, 3, 16);
        }
        playerInv(playerInv, 26, 134);
    }
}
