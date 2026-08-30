package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineMixerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code ContainerMixer} - coordinates kept for the slots that survive this pass's
 * battery/solid-input/upgrade layout (see {@link MachineMixerBlockEntity}'s own javadoc for the
 * fluid-identifier slot trim): battery at (23,77), item input at (43,77), 2 upgrade slots stacked
 * vertically at (137,24)/(137,42), player inventory 3 rows from y=122 + hotbar at y=180.
 */
public class MachineMixerMenu extends MenuBase<MachineMixerBlockEntity> {

    public MachineMixerMenu(int id, Inventory playerInv, MachineMixerBlockEntity be) {
        super(ProcessingMenus.MACHINE_MIXER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.BATTERY_SLOT, 23, 77));
        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.SOLID_INPUT, 43, 77));
        addSlots(tile, MachineMixerBlockEntity.UPGRADE_START, 137, 24, 2, 1);

        playerInv(playerInv, 8, 122, 180);
    }
}
