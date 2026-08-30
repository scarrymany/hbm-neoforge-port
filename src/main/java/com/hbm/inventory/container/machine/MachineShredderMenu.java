package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineShredderBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code ContainerMachineShredder} - slot coordinates copied verbatim (read in
 * full): input 3x3 at (44,18) step 18, output 6x3 (take-only) at (116,18) step 18, blade slots at
 * (44,108)/(80,108), battery at (8,108), player inventory 3 rows from y=151 + hotbar at y=209.
 * <p>
 * CE's own {@code TransferStrategy}-based shift-click routing (input/output/blade/battery kept in
 * their own sub-ranges) is not reproduced - {@link MenuBase#quickMoveStack} already documents this
 * exact trim as an accepted, deliberate scope cut for every Phase 2 machine menu (see that class's
 * own javadoc), not something specific to this machine.
 */
public class MachineShredderMenu extends MenuBase<MachineShredderBlockEntity> {

    public MachineShredderMenu(int id, Inventory playerInv, MachineShredderBlockEntity be) {
        super(ProcessingMenus.MACHINE_SHREDDER.get(), id, be);

        addSlots(tile, 0, 44, 18, 3, 3);
        addTakeOnlySlots(tile, 9, 116, 18, 6, 3);
        // (6 rows x 3 cols - matches CE's ContainerMachineShredder slot-by-slot coordinates exactly:
        // 3 columns at x=116/134/152, 6 rows at y=18/36/54/72/90/108)
        this.addSlot(new SlotNonRetarded(tile, 27, 44, 108));
        this.addSlot(new SlotNonRetarded(tile, 28, 80, 108));
        this.addSlot(new SlotNonRetarded(tile, 29, 8, 108));

        playerInv(playerInv, 8, 151, 209);
    }
}
