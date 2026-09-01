package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterHeatexBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerHeaterHeatex}: ID (80, 72). */
public class HeaterHeatexMenu extends MenuBase<HeaterHeatexBlockEntity> {

    public static final int BUTTON_COOL_UP = 0;
    public static final int BUTTON_COOL_DOWN = 1;
    public static final int BUTTON_DELAY_UP = 2;
    public static final int BUTTON_DELAY_DOWN = 3;

    public HeaterHeatexMenu(int id, Inventory playerInv, HeaterHeatexBlockEntity be) {
        super(DummyableProcessMenus.HEATER_HEATEX.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 72));
        playerInv(playerInv, 8, 122);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_COOL_UP) {
            be.bumpCool(100);
            return true;
        }
        if (id == BUTTON_COOL_DOWN) {
            be.bumpCool(-100);
            return true;
        }
        if (id == BUTTON_DELAY_UP) {
            be.bumpDelay(1);
            return true;
        }
        if (id == BUTTON_DELAY_DOWN) {
            be.bumpDelay(-1);
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
