package com.hbm.blocks;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

/**
 * Pairs a drop-item function with a fortune-aware drop-quantity function for an ore family.
 * Implemented by {@code OreEnumUtil.OreEnum} (its actual entries are Phase 1 content, ported once
 * {@code com.hbm.items.ModItems} exists).
 */
public interface IOreType {

    BiFunction<BlockState, RandomSource, ItemStack> getDropFunction();

    TriFunction<BlockState, Integer, RandomSource, Integer> getQuantityFunction();

    /**
     * {@code java.util.function} has no three-argument function; CE's equivalent lived in
     * {@code com.hbm.lib.TriFunction}. Declared locally here rather than depending on that
     * out-of-scope package for a single-method functional interface.
     */
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
