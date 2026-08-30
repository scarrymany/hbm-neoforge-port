package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ammo loot crate, ported from CE's {@code BlockAmmoCrate}. CE hardcodes a fixed cap/stimpak drop
 * plus a random subset of ~14 {@code GunFactory.EnumAmmo} variants; none of those ammo items exist
 * in the port's item catalog yet (weapons area, not part of this pass). The extra-drop list is left
 * empty and extensible via {@link #addExtraDrop} rather than guessing field names, matching
 * {@link BlockCrate}'s documented gap; the guaranteed drops (caps, stimpaks) are wired once those
 * concrete items exist.
 */
public class BlockAmmoCrate extends Block {

    private static final List<Supplier<ItemStack>> EXTRA_DROPS = new ArrayList<>();

    public BlockAmmoCrate(Properties properties) {
        super(properties);
    }

    /** Registers one more possible extra drop, rolled independently at a coin-flip chance. */
    public static void addExtraDrop(Supplier<ItemStack> stack) {
        EXTRA_DROPS.add(stack);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        RandomSource random = params.getLevel().getRandom();
        List<ItemStack> drops = new ArrayList<>();

        for (Supplier<ItemStack> supplier : EXTRA_DROPS) {
            if (random.nextBoolean()) {
                drops.add(supplier.get());
            }
        }

        return drops;
    }
}
