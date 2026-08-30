package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineRTGBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code ContainerMachineRTG} (read in full): the 15-slot pellet grid, laid out as
 * a 5x3 block at the exact pixel coordinates CE used ({@code 26,22} start, 18px pitch), so the
 * eventual GUI texture (not ported this pass, see {@code GuiInfoContainer}'s own asset-gap note)
 * lines up unchanged. CE's {@code heat} window-property sync is dropped: this port's block entities
 * sync their full state (including {@code heat}) over {@link com.hbm.blockentity.LoadedBaseBlockEntity}'s
 * own NBT/bytebuf sync pair instead, per {@link MenuBase}'s own class javadoc.
 */
public class MachineRTGMenu extends MenuBase<MachineRTGBlockEntity> {

    public MachineRTGMenu(int id, Inventory playerInv, MachineRTGBlockEntity be) {
        super(PowerGenMenus.MACHINE_RTG.get(), id, be);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                this.addSlot(new SlotNonRetarded(tile, col + row * 5, 26 + col * 18, 22 + row * 18));
            }
        }

        playerInv(playerInv, 8, 94);
    }
}
