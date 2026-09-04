package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineCombustionEngineBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Exact CE {@code ContainerCombustionEngine.java:37-41}: canister 17,17 / empty 17,53 / piston 88,71
 * / battery 143,71 / ID 35,71. {@code loadTank(0,1)} / {@code setType(4)} Exact CE
 * {@code TileEntityMachineCombustionEngine.java:96-99}. Buttons: 0 = on/off, 1/2 = throttle down/up
 * (CE's GUI used a scrollbar-style slider; a stepped button pair reaches the same 0-30 range
 * without needing custom widget/drag code).
 */
public class MachineCombustionEngineMenu extends MenuBase<MachineCombustionEngineBlockEntity> {

    public static final int BUTTON_TOGGLE_ON = 0;
    public static final int BUTTON_THROTTLE_DOWN = 1;
    public static final int BUTTON_THROTTLE_UP = 2;

    public MachineCombustionEngineMenu(int id, Inventory playerInv, MachineCombustionEngineBlockEntity be) {
        super(PowerGenMenus.COMBUSTION_ENGINE.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 17, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 17, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 88, 71));
        this.addSlot(new SlotNonRetarded(tile, 3, 143, 71));
        this.addSlot(new SlotNonRetarded(tile, 4, 35, 71));

        playerInv(playerInv, 8, 121, 179);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case BUTTON_TOGGLE_ON -> {
                be.setOn(!be.isOn);
                return true;
            }
            case BUTTON_THROTTLE_DOWN -> {
                be.setThrottle(be.setting - 1);
                return true;
            }
            case BUTTON_THROTTLE_UP -> {
                be.setThrottle(be.setting + 1);
                return true;
            }
            default -> {
                return super.clickMenuButton(player, id);
            }
        }
    }
}
