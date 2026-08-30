package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineCrystallizerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code ContainerCrystallizer} - coordinates kept for the slots that survive this
 * pass's item-input/battery/output/upgrade layout (see {@link MachineCrystallizerBlockEntity}'s own
 * javadoc for the fluid-load/fluid-id slot trim): item input at (62,45), battery at (152,72), item
 * output (take-only) at (113,45), 2 upgrade slots at (80,18)/(98,18), player inventory 3 rows from
 * y=122 + hotbar at y=180.
 */
public class MachineCrystallizerMenu extends MenuBase<MachineCrystallizerBlockEntity> {

    public MachineCrystallizerMenu(int id, Inventory playerInv, MachineCrystallizerBlockEntity be) {
        super(ProcessingMenus.MACHINE_CRYSTALLIZER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.ITEM_INPUT, 62, 45));
        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.BATTERY_SLOT, 152, 72));
        this.addSlot(new SlotTakeOnly(tile, MachineCrystallizerBlockEntity.ITEM_OUTPUT, 113, 45));
        addSlots(tile, MachineCrystallizerBlockEntity.UPGRADE_START, 80, 18, 1, 2);

        playerInv(playerInv, 8, 122, 180);
    }
}
