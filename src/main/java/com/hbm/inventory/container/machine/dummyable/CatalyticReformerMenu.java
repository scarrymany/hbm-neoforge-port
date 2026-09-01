package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCatalyticReformerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineCatalyticReformer} 11-slot layout. */
public class CatalyticReformerMenu extends MenuBase<MachineCatalyticReformerBlockEntity> {

    public CatalyticReformerMenu(int id, Inventory playerInv, MachineCatalyticReformerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_CATALYTIC_REFORMER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 90));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 90));
        this.addSlot(new SlotTakeOnly(tile, 2, 35, 108));
        this.addSlot(new SlotNonRetarded(tile, 3, 107, 90));
        this.addSlot(new SlotTakeOnly(tile, 4, 107, 108));
        this.addSlot(new SlotNonRetarded(tile, 5, 125, 90));
        this.addSlot(new SlotTakeOnly(tile, 6, 125, 108));
        this.addSlot(new SlotNonRetarded(tile, 7, 143, 90));
        this.addSlot(new SlotTakeOnly(tile, 8, 143, 108));
        this.addSlot(new SlotNonRetarded(tile, 9, 17, 108));
        this.addSlot(new SlotNonRetarded(tile, 10, 71, 36));
        playerInv(playerInv, 8, 156, 214);
    }
}
