package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineCrystallizerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerCrystallizer.java:35-42}: input 62,45 / battery 152,72 / output 113,45 /
 * canister 17,18 + 17,54 takeOnly / upgrades 80,18 + 98,18 / ID 35,72.
 * {@code setType(7)} / {@code loadTank(3,4)} Exact CE {@code TileEntityMachineCrystallizer.java:133-134}.
 */
public class MachineCrystallizerMenu extends MenuBase<MachineCrystallizerBlockEntity> {

    public MachineCrystallizerMenu(int id, Inventory playerInv, MachineCrystallizerBlockEntity be) {
        super(ProcessingMenus.MACHINE_CRYSTALLIZER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.ITEM_INPUT, 62, 45));
        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.BATTERY_SLOT, 152, 72));
        this.addSlot(new SlotTakeOnly(tile, MachineCrystallizerBlockEntity.ITEM_OUTPUT, 113, 45));
        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.SLOT_CANISTER, 17, 18));
        this.addSlot(new SlotTakeOnly(tile, MachineCrystallizerBlockEntity.SLOT_EMPTY, 17, 54));
        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.UPGRADE_START, 80, 18));
        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.UPGRADE_END, 98, 18));
        this.addSlot(new SlotNonRetarded(tile, MachineCrystallizerBlockEntity.SLOT_ID, 35, 72));

        playerInv(playerInv, 8, 122, 180);
    }
}
