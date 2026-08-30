package com.hbm.inventory.container.machine.oil;

import com.hbm.blockentity.machine.oil.MachineRefineryBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (slot layout trimmed, see {@link MachineRefineryBlockEntity}'s javadoc) from CE's
 * {@code ContainerMachineRefinery}: battery + sulfur-byproduct output slot only (CE: battery, 5
 * canister in/out pairs, sulfur out, fluid-ID slot - the canister slots are dropped along with the
 * item-container fluid-loading mechanic, same as every other machine in this area).
 */
public class MachineRefineryMenu extends MenuBase<MachineRefineryBlockEntity> {

    public MachineRefineryMenu(int id, Inventory playerInv, MachineRefineryBlockEntity be) {
        super(OilChainMenus.MACHINE_REFINERY.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 186, 72));
        this.addSlot(new SlotTakeOnly(tile, 1, 58, 119));

        playerInv(playerInv, 8, 150, 208);
    }
}
