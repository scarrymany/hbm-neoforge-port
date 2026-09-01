package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineForceFieldBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerForceField}: battery 26,53 / radius 89,35 / health 107,35. */
public class ForceFieldMenu extends MenuBase<MachineForceFieldBlockEntity> {

    public static final int BUTTON_ON = 0;

    public ForceFieldMenu(int id, Inventory playerInv, MachineForceFieldBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_FORCEFIELD.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 53));
        this.addSlot(new SlotNonRetarded(tile, 1, 89, 35));
        this.addSlot(new SlotNonRetarded(tile, 2, 107, 35));
        playerInv(playerInv, 8, 84);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ON) {
            be.toggleOn();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
