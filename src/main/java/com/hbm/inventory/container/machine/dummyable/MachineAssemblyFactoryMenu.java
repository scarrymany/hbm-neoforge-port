package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAssemblyFactoryBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code ContainerMachineAssemblyFactory}: battery 234,112; upgrades 214,149/167/185;
 * 4 lanes template + 2×6 inputs @ 16px + output; playerInv 33,158.
 */
public class MachineAssemblyFactoryMenu extends MenuBase<MachineAssemblyFactoryBlockEntity> {

    public MachineAssemblyFactoryMenu(int id, Inventory playerInv, MachineAssemblyFactoryBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ASSEMBLY_FACTORY.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 234, 112));
        this.addSlot(new SlotNonRetarded(tile, 1, 214, 149));
        this.addSlot(new SlotNonRetarded(tile, 2, 214, 167));
        this.addSlot(new SlotNonRetarded(tile, 3, 214, 185));
        for (int i = 0; i < 4; i++) {
            addSlots(tile, MachineAssemblyFactoryBlockEntity.blueprintSlot(i), 25 + (i % 2) * 109, 54 + (i / 2) * 56, 1, 1);
            addSlots(tile, MachineAssemblyFactoryBlockEntity.inputStart(i), 7 + (i % 2) * 109, 20 + (i / 2) * 56, 2, 6, 16);
            addOutputSlots(playerInv.player, tile, MachineAssemblyFactoryBlockEntity.outputSlot(i), 87 + (i % 2) * 109, 54 + (i / 2) * 56, 1, 1);
        }
        playerInv(playerInv, 33, 158);
    }
}
