package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAutosawBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE overlay-only Autosaw → live tank + suspend. */
public class AutosawMenu extends MenuBase<MachineAutosawBlockEntity> {

    public static final int BUTTON_SUSPEND = 0;

    public AutosawMenu(int id, Inventory playerInv, MachineAutosawBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_AUTOSAW.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 86);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_SUSPEND) {
            be.toggleSuspended();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
