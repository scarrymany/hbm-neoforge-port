package com.hbm.items.special;

import com.hbm.hazard.modifier.IHazardModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Yields a fixed hazard level only when the stack's {@link SpecialItemComponents#CELL_FLUID_ID}
 * component matches {@link #fluidId}, otherwise contributes nothing. Needed because {@code hbm:cell}
 * is a single item across every fluid (see {@link ItemCell}'s javadoc) rather than one flattened
 * item per fluid: CE bound per-fluid hazards to specific {@code ItemStack(cell, 1, meta)} pairs
 * (e.g. tritium, SAS3), which no longer maps to a distinct registry key post-flattening. Registering
 * one entry per radioactive fluid, each gated by this modifier, reproduces the same per-fluid
 * behavior against the single item.
 */
public class CellFluidHazardModifier implements IHazardModifier {

    private final int fluidId;
    private final double level;

    public CellFluidHazardModifier(int fluidId, double level) {
        this.fluidId = fluidId;
        this.level = level;
    }

    @Override
    public double modify(ItemStack stack, LivingEntity holder, double level) {
        return ItemCell.getFluidId(stack) == fluidId ? this.level : 0D;
    }
}
