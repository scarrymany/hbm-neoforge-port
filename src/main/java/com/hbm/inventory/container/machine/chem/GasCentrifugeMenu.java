package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.GasCentrifugeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineGasCent}: outputs 0–3 2×2 at 71+j*18, 53+i*18 ({@code :37-41}),
 * battery 4 @ 182,71 ({@code :45}), fluid-ID 5 @ 91,15 ({@code :48}), upgrade 6 @ 69,15 ({@code :51}),
 * playerInv 8,122 / hotbar 180. Existing {@code gui_centrifuge_gas.png} — not invent.
 */
public class GasCentrifugeMenu extends MenuBase<GasCentrifugeBlockEntity> {

    public GasCentrifugeMenu(int id, Inventory playerInv, GasCentrifugeBlockEntity be) {
        super(ChemIsotopeMenus.GAS_CENTRIFUGE.get(), id, be);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                this.addSlot(new SlotTakeOnly(tile, j + i * 2, 71 + j * 18, 53 + i * 18));
            }
        }
        this.addSlot(new SlotNonRetarded(tile, GasCentrifugeBlockEntity.BATTERY_SLOT, 182, 71));
        this.addSlot(new SlotNonRetarded(tile, GasCentrifugeBlockEntity.SLOT_ID, 91, 15));
        this.addSlot(new SlotNonRetarded(tile, GasCentrifugeBlockEntity.UPGRADE_SLOT, 69, 15));

        playerInv(playerInv, 8, 122);
    }
}
