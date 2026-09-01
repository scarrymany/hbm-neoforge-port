package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerOilburner}: fluid 26,18+36 / ID 80,54. On + setting buttons. */
public class OilburnerMenu extends MenuBase<HeaterOilburnerBlockEntity> {

    public static final int BUTTON_ON = 0;
    public static final int BUTTON_UP = 1;
    public static final int BUTTON_DOWN = 2;

    public OilburnerMenu(int id, Inventory playerInv, HeaterOilburnerBlockEntity be) {
        super(DummyableProcessMenus.HEATER_OILBURNER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 18));
        this.addSlot(new SlotTakeOnly(tile, 1, 26, 54));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 54));
        playerInv(playerInv, 8, 86);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ON) {
            be.toggleOn();
            return true;
        }
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
