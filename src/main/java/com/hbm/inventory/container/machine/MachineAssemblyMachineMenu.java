package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineAssemblyMachineBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotCraftingOutput;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code ContainerMachineAssemblyMachine} - slot coordinates copied verbatim: battery
 * at (152,81), blueprint slot at (35,126), 2 upgrade slots at (152,108)/(170,108), 12 input slots as a
 * 4-col x 3-row grid from (8,18), 1 output slot at (98,45), player inventory from (8,174).
 */
public class MachineAssemblyMachineMenu extends MenuBase<MachineAssemblyMachineBlockEntity> {

    public MachineAssemblyMachineMenu(int id, Inventory playerInv, MachineAssemblyMachineBlockEntity be) {
        super(ProcessingMenus.MACHINE_ASSEMBLER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, MachineAssemblyMachineBlockEntity.BATTERY_SLOT, 152, 81));
        this.addSlot(new SlotNonRetarded(tile, MachineAssemblyMachineBlockEntity.BLUEPRINT_SLOT, 35, 126));
        addSlots(tile, MachineAssemblyMachineBlockEntity.UPGRADE_START, 152, 108, 1, 2);
        addSlots(tile, MachineAssemblyMachineBlockEntity.INPUT_START, 8, 18, 3, 4);
        this.addSlot(new SlotCraftingOutput(playerInv.player, tile, MachineAssemblyMachineBlockEntity.OUTPUT_SLOT, 98, 45));

        playerInv(playerInv, 8, 174);
    }
}
