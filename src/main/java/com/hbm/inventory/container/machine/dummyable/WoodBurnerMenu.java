package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineWoodBurnerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerMachineWoodBurner}: fuel 26,18 / ash 26,54 / ID 98,54 / fluid 98,18+36 / bat 143,54. */
public class WoodBurnerMenu extends MenuBase<MachineWoodBurnerBlockEntity> {

    public static final int BUTTON_ON = 0;
    public static final int BUTTON_LIQUID = 1;

    public WoodBurnerMenu(int id, Inventory playerInv, MachineWoodBurnerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_WOOD_BURNER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 18));
        this.addSlot(new SlotTakeOnly(tile, 1, 26, 54));
        this.addSlot(new SlotNonRetarded(tile, 2, 98, 54));
        this.addSlot(new SlotNonRetarded(tile, 3, 98, 18));
        this.addSlot(new SlotTakeOnly(tile, 4, 98, 36));
        this.addSlot(new SlotNonRetarded(tile, 5, 143, 54));
        playerInv(playerInv, 8, 104, 162);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ON) {
            be.toggleOn();
            return true;
        }
        if (id == BUTTON_LIQUID) {
            be.toggleLiquid();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
