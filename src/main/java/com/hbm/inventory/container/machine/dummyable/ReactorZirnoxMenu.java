package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.ReactorZirnoxBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerReactorZirnox}: 24 rods + CO2/water IO. GUI 203×256. */
public class ReactorZirnoxMenu extends MenuBase<ReactorZirnoxBlockEntity> {

    public static final int BUTTON_CONTROL = 0;
    public static final int BUTTON_VENT = 1;

    public ReactorZirnoxMenu(int id, Inventory playerInv, ReactorZirnoxBlockEntity be) {
        super(DummyableProcessMenus.REACTOR_ZIRNOX.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 16));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 16));
        this.addSlot(new SlotNonRetarded(tile, 2, 98, 16));
        this.addSlot(new SlotNonRetarded(tile, 3, 8, 34));
        this.addSlot(new SlotNonRetarded(tile, 4, 44, 34));
        this.addSlot(new SlotNonRetarded(tile, 5, 80, 34));
        this.addSlot(new SlotNonRetarded(tile, 6, 116, 34));
        this.addSlot(new SlotNonRetarded(tile, 7, 26, 52));
        this.addSlot(new SlotNonRetarded(tile, 8, 62, 52));
        this.addSlot(new SlotNonRetarded(tile, 9, 98, 52));
        this.addSlot(new SlotNonRetarded(tile, 10, 8, 70));
        this.addSlot(new SlotNonRetarded(tile, 11, 44, 70));
        this.addSlot(new SlotNonRetarded(tile, 12, 80, 70));
        this.addSlot(new SlotNonRetarded(tile, 13, 116, 70));
        this.addSlot(new SlotNonRetarded(tile, 14, 26, 88));
        this.addSlot(new SlotNonRetarded(tile, 15, 62, 88));
        this.addSlot(new SlotNonRetarded(tile, 16, 98, 88));
        this.addSlot(new SlotNonRetarded(tile, 17, 8, 106));
        this.addSlot(new SlotNonRetarded(tile, 18, 44, 106));
        this.addSlot(new SlotNonRetarded(tile, 19, 80, 106));
        this.addSlot(new SlotNonRetarded(tile, 20, 116, 106));
        this.addSlot(new SlotNonRetarded(tile, 21, 26, 124));
        this.addSlot(new SlotNonRetarded(tile, 22, 62, 124));
        this.addSlot(new SlotNonRetarded(tile, 23, 98, 124));
        this.addSlot(new SlotNonRetarded(tile, 24, 143, 124));
        this.addSlot(new SlotNonRetarded(tile, 25, 179, 124));
        this.addSlot(new SlotTakeOnly(tile, 26, 143, 142));
        this.addSlot(new SlotTakeOnly(tile, 27, 179, 142));
        playerInv(playerInv, 8, 174, 232);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CONTROL) {
            be.toggleControl();
            return true;
        }
        if (id == BUTTON_VENT) {
            be.vent();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
