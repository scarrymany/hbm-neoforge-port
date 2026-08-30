package com.hbm.inventory.container.machine.oil;

import com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (slot layout trimmed, see {@link OilDrillBaseBlockEntity}'s javadoc) from CE's
 * {@code ContainerMachineOilWell} - the one GUI/Container pair shared by all three extractors
 * (derrick, pumpjack, fracking tower), exactly like CE (each concrete extractor's
 * {@code createMenu} - implemented once, on the shared base class - opens this same Menu type).
 * Canister in/out slot pairs dropped along with the item-container fluid-loading mechanic; battery
 * and upgrade slots kept at repositioned pixel coordinates.
 */
public class MachineOilWellMenu extends MenuBase<OilDrillBaseBlockEntity> {

    public MachineOilWellMenu(int id, Inventory playerInv, OilDrillBaseBlockEntity be) {
        super(OilChainMenus.MACHINE_OIL_WELL.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 8, 58));
        this.addSlot(new SlotNonRetarded(tile, 1, 138, 22));
        this.addSlot(new SlotNonRetarded(tile, 2, 138, 40));
        this.addSlot(new SlotNonRetarded(tile, 3, 138, 58));

        playerInv(playerInv, 8, 121, 179);
    }
}
