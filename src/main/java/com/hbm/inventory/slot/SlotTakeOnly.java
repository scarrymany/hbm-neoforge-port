package com.hbm.inventory.slot;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Ported from CE's {@code com.hbm.inventory.slot.SlotTakeOnly} (read in full) - an
 * {@link IItemHandler}-backed slot that never accepts a player-placed item, used for machine
 * output slots. Cross-checked against Neo Edition's real {@code com.hbm.inventory.SlotTakeOnly}
 * (vanilla-{@code Container}-backed there, since Neo Edition's block entities implement
 * {@code net.minecraft.world.Container} directly rather than exposing an {@link IItemHandler} -
 * this port's {@code MachineBaseBlockEntity} does the latter, so this class binds to
 * {@link IItemHandler} like CE's original instead) - same single {@code mayPlace() -> false}
 * override either way.
 */
public class SlotTakeOnly extends SlotItemHandler {

    public SlotTakeOnly(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
