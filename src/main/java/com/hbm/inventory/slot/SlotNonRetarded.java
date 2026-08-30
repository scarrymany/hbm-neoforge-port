package com.hbm.inventory.slot;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Ported from CE's {@code com.hbm.inventory.slot.SlotNonRetarded} (read in full) - a plain
 * {@link IItemHandler}-backed slot whose only behavior change from a stock
 * {@code net.neoforged.neoforge.items.SlotItemHandler} is not lying about the slot's max stack
 * size when the handler allows oversized stacks (CE's own comment, kept verbatim below).
 * Method names are retyped to their 1.21.1 Mojang-mapped equivalents: CE's {@code getSlotStackLimit}
 * is this port's {@link #getMaxStackSize()}, CE's {@code getHasStack}/{@code getStack} are
 * {@link #hasItem()}/{@link #getItem()}.
 *
 * <p>This is the slot class {@code com.hbm.inventory.container.MenuBase}'s {@code addSlots}/
 * {@code playerInv} batch helpers use for every plain machine-inventory slot - see
 * {@code docs/phase2/gui_framework.md}'s Phase-2-safe scope, bullet "Slot classes".
 *
 * <p><b>Build-time note</b> (this sandbox has no NeoForge artifact cache to decompile and confirm
 * against): constructed against {@code net.neoforged.neoforge.items.SlotItemHandler}, the same
 * package/shape NeoForge carries forward unchanged from Forge's own
 * {@code net.minecraftforge.items.SlotItemHandler} (which CE itself extends, and which this port's
 * own {@code com.hbm.blockentity.MachineBaseBlockEntity}/{@code ItemStackHandlerWrapper} already
 * depend on the sibling {@code IItemHandlerModifiable} interface from the same package existing) -
 * double check the {@code (IItemHandler, int, int, int)} constructor and the
 * {@link #getMaxStackSize()} override point against the real class on first build.
 */
public class SlotNonRetarded extends SlotItemHandler {

    public SlotNonRetarded(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    /**
     * Because if slots have higher stack sizes than the maximum allowed by the tile, the display
     * just stops working. Why was that necessary? Sure it's not intended but falsifying
     * information isn't very cool.
     */
    @Override
    public int getMaxStackSize() {
        return Math.max(super.getMaxStackSize(), this.hasItem() ? this.getItem().getCount() : 1);
    }
}
