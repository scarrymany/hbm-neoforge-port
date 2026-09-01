package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineMicrowaveBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerMicrowave}: in / out + speed buttons. Battery slot unused. */
public class MicrowaveMenu extends MenuBase<MachineMicrowaveBlockEntity> {

    public static final int BUTTON_UP = 0;
    public static final int BUTTON_DOWN = 1;

    public MicrowaveMenu(int id, Inventory playerInv, MachineMicrowaveBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_MICROWAVE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 35));
        this.addSlot(new SlotTakeOnly(tile, 1, 80, 55));
        playerInv(playerInv, 8, 86);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_UP) {
            be.bumpSpeed(1);
            return true;
        }
        if (id == BUTTON_DOWN) {
            be.bumpSpeed(-1);
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
