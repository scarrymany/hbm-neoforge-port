package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineTurbineGasBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Ported (slot layout trimmed, see {@link MachineTurbineGasBlockEntity}'s javadoc) from CE's
 * {@code ContainerMachineTurbineGas}: only the battery-charging slot survives (CE's slot 0), kept at
 * CE's own pixel position; the fluid-identifier item slot (CE's slot 1) is dropped along with the
 * fluid-retyping mechanic. Buttons: 0 = start/stop, 1 = auto-mode toggle, 2/3 = throttle down/up.
 */
public class MachineTurbineGasMenu extends MenuBase<MachineTurbineGasBlockEntity> {

    public static final int BUTTON_TOGGLE_RUN = 0;
    public static final int BUTTON_TOGGLE_AUTO = 1;
    public static final int BUTTON_THROTTLE_DOWN = 2;
    public static final int BUTTON_THROTTLE_UP = 3;

    public MachineTurbineGasMenu(int id, Inventory playerInv, MachineTurbineGasBlockEntity be) {
        super(PowerGenMenus.MACHINE_TURBINE_GAS.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 8, 109));

        playerInv(playerInv, 8, 141, 199);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case BUTTON_TOGGLE_RUN -> {
                be.setRunning(be.state != 1);
                return true;
            }
            case BUTTON_TOGGLE_AUTO -> {
                be.setAutoMode(!be.autoMode);
                return true;
            }
            case BUTTON_THROTTLE_DOWN -> {
                be.setSliderPos(be.powerSliderPos - 1);
                return true;
            }
            case BUTTON_THROTTLE_UP -> {
                be.setSliderPos(be.powerSliderPos + 1);
                return true;
            }
            default -> {
                return super.clickMenuButton(player, id);
            }
        }
    }
}
