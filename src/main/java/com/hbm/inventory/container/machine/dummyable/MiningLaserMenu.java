package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineMiningLaserBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * CE {@code ContainerMachineMiningLaser}: battery 8,108 / upgrades 2×4 / output 3×7 /
 * player inv y+56.
 */
public class MiningLaserMenu extends MenuBase<MachineMiningLaserBlockEntity> {

    public static final int BUTTON_ON = 0;

    public MiningLaserMenu(int id, Inventory playerInv, MachineMiningLaserBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_MINING_LASER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 108));
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new SlotNonRetarded(tile, 1 + i * 4 + j, 98 + j * 18, 18 + i * 18));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                this.addSlot(new SlotNonRetarded(tile, 9 + i * 7 + j, 44 + j * 18, 72 + i * 18));
            }
        }
        playerInv(playerInv, 8, 140);
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
