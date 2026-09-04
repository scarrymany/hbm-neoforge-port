package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineMixerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMixer.java:32-40}: battery 23,77 / solid 43,77 / ID 117,77 /
 * upgrades 137,24 + 137,42. {@code setType(2)} Exact CE {@code TileEntityMachineMixer.java:95}.
 */
public class MachineMixerMenu extends MenuBase<MachineMixerBlockEntity> {

    public MachineMixerMenu(int id, Inventory playerInv, MachineMixerBlockEntity be) {
        super(ProcessingMenus.MACHINE_MIXER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.BATTERY_SLOT, 23, 77));
        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.SOLID_INPUT, 43, 77));
        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.SLOT_ID, 117, 77));
        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.UPGRADE_START, 137, 24));
        this.addSlot(new SlotNonRetarded(tile, MachineMixerBlockEntity.UPGRADE_END, 137, 42));

        playerInv(playerInv, 8, 122, 180);
    }
}
