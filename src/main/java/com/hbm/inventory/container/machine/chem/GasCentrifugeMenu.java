package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.GasCentrifugeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineGasCent}: fluid-ID slot 5 @ 91,15 ({@code :48}).
 * Upgrade is slot 6 (CE {@code :51}); battery stays at the playable 176-wide panel coords
 * (CE battery 182,71 needs {@code gui_centrifuge_gas.png}, which is not in this tree).
 */
public class GasCentrifugeMenu extends MenuBase<GasCentrifugeBlockEntity> {

    public GasCentrifugeMenu(int id, Inventory playerInv, GasCentrifugeBlockEntity be) {
        super(ChemIsotopeMenus.GAS_CENTRIFUGE.get(), id, be);

        this.addSlot(new SlotTakeOnly(tile, 0, 44, 21));
        this.addSlot(new SlotTakeOnly(tile, 1, 44, 39));
        this.addSlot(new SlotTakeOnly(tile, 2, 44, 57));
        this.addSlot(new SlotTakeOnly(tile, 3, 44, 75));
        this.addSlot(new SlotNonRetarded(tile, GasCentrifugeBlockEntity.BATTERY_SLOT, 116, 40));
        this.addSlot(new SlotNonRetarded(tile, GasCentrifugeBlockEntity.SLOT_ID, 91, 15));
        this.addSlot(new SlotNonRetarded(tile, GasCentrifugeBlockEntity.UPGRADE_SLOT, 152, 40));

        playerInv(playerInv, 8, 116);
    }
}
