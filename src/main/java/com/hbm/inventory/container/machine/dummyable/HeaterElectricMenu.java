package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterElectricBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE overlay-only heater_electric → live setting menu. */
public class HeaterElectricMenu extends MenuBase<HeaterElectricBlockEntity> {

    public static final int BUTTON_UP = 0;
    public static final int BUTTON_DOWN = 1;

    public HeaterElectricMenu(int id, Inventory playerInv, HeaterElectricBlockEntity be) {
        super(DummyableProcessMenus.HEATER_ELECTRIC.get(), id, be);
        playerInv(playerInv, 8, 86);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_UP) {
            be.bumpSetting(1);
            return true;
        }
        if (id == BUTTON_DOWN) {
            be.bumpSetting(-1);
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
