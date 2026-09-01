package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HexTankBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Tank inspect (CE UF6/PuF6 canister slots skipped). */
public class HexTankMenu extends MenuBase<HexTankBlockEntity> {

    public HexTankMenu(int id, Inventory playerInv, HexTankBlockEntity be) {
        super(DummyableProcessMenus.HEX_TANK.get(), id, be);
        playerInv(playerInv, 8, 84);
    }
}
