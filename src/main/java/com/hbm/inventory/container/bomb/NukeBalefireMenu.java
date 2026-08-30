package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeBalefireBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Menu for {@link NukeBalefireBlockEntity} - 2 slots (egg, battery) plus arm/timer buttons. CE's
 * {@code GUIFstBmb} used a free-drag timer slider sending {@code handleButtonPacket} NBT packets
 * directly; substituted here with the same stepped-button convention this port already established
 * for an identical slider-replacement problem (see {@code PWRControllerMenu}'s own javadoc) - a
 * fixed +/-60s step and a start button, both routed through
 * {@link NukeBalefireBlockEntity#handleButtonPacket(int, int)} exactly like CE's own dispatch.
 */
public class NukeBalefireMenu extends MenuBase<NukeBalefireBlockEntity> {

    public static final int BUTTON_START = 0;
    public static final int BUTTON_TIMER_DOWN = 1;
    public static final int BUTTON_TIMER_UP = 2;
    private static final int TIMER_STEP_SECONDS = 60;

    public NukeBalefireMenu(int id, Inventory playerInv, NukeBalefireBlockEntity be) {
        super(NukeCasingMenus.NUKE_BALEFIRE.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 62, 24));
        this.addSlot(new SlotNonRetarded(tile, 1, 98, 24));

        playerInv(playerInv, 8, 84);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case BUTTON_START -> {
                be.handleButtonPacket(0, NukeBalefireBlockEntity.BUTTON_START);
                return true;
            }
            case BUTTON_TIMER_DOWN -> {
                int seconds = Math.max(0, be.timer / 20 - TIMER_STEP_SECONDS);
                be.handleButtonPacket(seconds, NukeBalefireBlockEntity.BUTTON_SET_TIMER);
                return true;
            }
            case BUTTON_TIMER_UP -> {
                int seconds = be.timer / 20 + TIMER_STEP_SECONDS;
                be.handleButtonPacket(seconds, NukeBalefireBlockEntity.BUTTON_SET_TIMER);
                return true;
            }
            default -> {
                return super.clickMenuButton(player, id);
            }
        }
    }
}
