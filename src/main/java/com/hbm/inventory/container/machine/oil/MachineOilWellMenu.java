package com.hbm.inventory.container.machine.oil;

import com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineOilWell.java:33-45}: battery 8,58 / oil 94,22 + 94,58 /
 * gas 130,22 + 130,58 / upgrades 156,36 + 156,54. Shared by derrick, pumpjack, fracking tower.
 * {@code unloadTank(1,2)} / {@code unloadTank(3,4)} Exact CE {@code TileEntityOilDrillBase.java:110-111}.
 */
public class MachineOilWellMenu extends MenuBase<OilDrillBaseBlockEntity> {

    public MachineOilWellMenu(int id, Inventory playerInv, OilDrillBaseBlockEntity be) {
        super(OilChainMenus.MACHINE_OIL_WELL.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 8, 58));
        this.addSlot(new SlotNonRetarded(tile, 1, 94, 22));
        this.addSlot(new SlotTakeOnly(tile, 2, 94, 58));
        this.addSlot(new SlotNonRetarded(tile, 3, 130, 22));
        this.addSlot(new SlotTakeOnly(tile, 4, 130, 58));
        this.addSlot(new SlotNonRetarded(tile, 5, 156, 36));
        this.addSlot(new SlotNonRetarded(tile, 6, 156, 54));

        playerInv(playerInv, 8, 121, 179);
    }
}
