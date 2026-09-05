package com.hbm.hazard.modifier;

import com.hbm.blocks.generic.BlockSellafield;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Exact CE {@code HazardRegistry.java:236-241} discrete {@code sellafield} meta rads
 * ({@code 0.5 / 1 / 2.5 / 4 / 5 / 10}). Port meta is {@code BLOCK_STATE} {@link BlockSellafield#LEVEL}.
 */
public class HazardModifierSellafield implements IHazardModifier {

    /** CE {@code :236-241} meta 0–5. */
    private static final float[] RADS = {0.5F, 1F, 2.5F, 4F, 5F, 10F};

    @Override
    public double modify(final ItemStack stack, final LivingEntity holder, final double level) {
        int meta = BlockSellafield.itemLevel(stack);
        if (meta < 0 || meta >= RADS.length) return RADS[0];
        return RADS[meta];
    }
}
