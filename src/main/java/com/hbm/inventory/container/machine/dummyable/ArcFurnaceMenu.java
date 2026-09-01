package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineArcFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerMachineArcFurnaceLarge}: 3 electrodes / battery / upgrade / 4×5 grid / 5 queue. */
public class ArcFurnaceMenu extends MenuBase<MachineArcFurnaceBlockEntity> {

    public static final int BUTTON_LIQUID = 0;

    public ArcFurnaceMenu(int id, Inventory playerInv, MachineArcFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ARC_FURNACE.get(), id, be);
        for (int i = 0; i < 3; i++) this.addSlot(new SlotNonRetarded(tile, i, 62 + i * 18, 22));
        this.addSlot(new SlotNonRetarded(tile, 3, 8, 108));
        this.addSlot(new SlotNonRetarded(tile, 4, 152, 108));
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                this.addSlot(new SlotNonRetarded(tile, 5 + j + i * 5, 44 + j * 18, 54 + i * 18));
            }
        }
        for (int i = 0; i < 5; i++) this.addSlot(new SlotNonRetarded(tile, 25 + i, 44 + i * 18, 129));
        playerInv(playerInv, 8, 174, 232);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_LIQUID) {
            be.toggleLiquid();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
