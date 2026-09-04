package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HexTankBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineUF6Tank}: slots 44,17 / 44,53 / 116,17 / 116,53. */
public class HexTankMenu extends MenuBase<HexTankBlockEntity> {

    public HexTankMenu(int id, Inventory playerInv, HexTankBlockEntity be) {
        super(DummyableProcessMenus.HEX_TANK.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 44, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 116, 17));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 53));
        playerInv(playerInv, 8, 84);
    }
}
