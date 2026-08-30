package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.world.item.ItemStack;

/**
 * Item-side fluid container contract (canisters, jetpacks, fluid-identifier tools). Ported unchanged
 * from CE aside from {@code net.minecraft.item.ItemStack} -&gt; {@code net.minecraft.world.item.ItemStack}
 * - a plain interface, no 1.12.2-specific types beyond that one import. Item-scoped; its consumers
 * (canister/jetpack items, per {@code docs/phase1/items_food_gear.md}'s {@code JetpackGlider} note)
 * are a different package's concern - ported here alongside the rest of {@code com.hbm.api.fluidmk2}
 * only because it is one of the trio's 12 files, not because this package implements it.
 */
public interface IFillableItem {

    /** Whether this stack can be filled with this type. Not particularly useful for normal operations. */
    boolean acceptsFluid(FluidType type, ItemStack stack);

    /** Tries to fill the stack, returns the remainder that couldn't be added. */
    int tryFill(FluidType type, int amount, ItemStack stack);

    /** Whether this stack can fill tiles with this type. Not particularly useful for normal operations. */
    boolean providesFluid(FluidType type, ItemStack stack);

    /** Provides fluid with the maximum being the requested amount. */
    int tryEmpty(FluidType type, int amount, ItemStack stack);

    /** Returns the first (or only) currently held type, may return null. Currently only used for setting bedrock ores. */
    FluidType getFirstFluidType(ItemStack stack);

    /** Returns the fillstate for the specified fluid. Currently only used for setting bedrock ores. */
    int getFill(ItemStack stack);
}
