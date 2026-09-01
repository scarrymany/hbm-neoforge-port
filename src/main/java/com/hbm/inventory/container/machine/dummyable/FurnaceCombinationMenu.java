package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceCombinationBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFurnaceCombo}: in 26,36 / out 89,36 / canister 136,18 / 136,54. */
public class FurnaceCombinationMenu extends MenuBase<FurnaceCombinationBlockEntity> {

    public FurnaceCombinationMenu(int id, Inventory playerInv, FurnaceCombinationBlockEntity be) {
        super(DummyableProcessMenus.FURNACE_COMBINATION.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 36));
        this.addSlot(new SlotTakeOnly(tile, 1, 89, 36));
        this.addSlot(new SlotNonRetarded(tile, 2, 136, 18));
        this.addSlot(new SlotTakeOnly(tile, 3, 136, 54));
        playerInv(playerInv, 8, 104);
    }
}
