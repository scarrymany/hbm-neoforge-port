package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineCrucibleBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Ported from CE's {@code ContainerCrucible} (89 lines, read in full) - slot coordinates copied
 * verbatim: input 3x3 (stack-limit-1 each) at {@code (107 + 18c, 18 + 18r)}, backing block-entity
 * inventory slots 1-9 (slot 0 is a dead slot - see {@link MachineCrucibleBlockEntity}'s own javadoc).
 * Player inventory 3 rows from {@code y=132}, hotbar at {@code y=190}.
 * <p>
 * CE's own {@code TransferStrategy}-based shift-click routing is not reproduced -
 * {@link MenuBase#quickMoveStack} already documents this exact trim as an accepted, deliberate scope
 * cut for every Phase 2 machine menu (see that class's own javadoc), not something specific to this
 * machine.
 */
public class MachineCrucibleMenu extends MenuBase<MachineCrucibleBlockEntity> {

    public MachineCrucibleMenu(int id, Inventory playerInv, MachineCrucibleBlockEntity be) {
        super(CrucibleMenus.MACHINE_CRUCIBLE.get(), id, be);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotOneItem(tile, col + row * 3 + 1, 107 + col * 18, 18 + row * 18));
            }
        }

        playerInv(playerInv, 8, 132, 190);
    }

    /** CE: {@code ContainerCrucible.SlotOneItem} - stack-limit-1 override, mirrored exactly. */
    public static class SlotOneItem extends SlotNonRetarded {
        public SlotOneItem(IItemHandler inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
