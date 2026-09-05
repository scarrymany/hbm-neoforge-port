package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineTurbofanBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineTurbofan}: canister 17,17 / empty 17,53 / upgrade 98,71 / bat 143,71 / ID 44,71.
 * Slot 0/1 {@code loadTank} + slot 4 {@code setType} Exact CE {@code :156-157}. */
public class TurbofanMenu extends MenuBase<MachineTurbofanBlockEntity> {

    public TurbofanMenu(int id, Inventory playerInv, MachineTurbofanBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_TURBOFAN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 17, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 98, 71));
        this.addSlot(new SlotNonRetarded(tile, 3, 143, 71));
        this.addSlot(new SlotNonRetarded(tile, 4, 44, 71));
        playerInv(playerInv, 8, 121, 179);
    }
}
