package com.hbm.blocks;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fortune-aware drop-quantity helper functions used by {@link IOreType} implementations.
 * <p>
 * CE's {@code OreEnumUtil} also carried a 30+ entry {@code OreEnum} implementing {@link IOreType}
 * with drop functions wired to {@code ModItems} fields (and, for a couple of entries, {@code ModBlocks}
 * and {@code ItemPool}). None of those exist yet in the port, so that enum is deliberately not ported
 * here: it is ore content for Phase 1 to add once {@code com.hbm.items.ModItems} exists. Only the
 * pure, dependency-free quantity math survives in this area.
 */
public class OreEnumUtil {

    public static int base2Rand3Fortune(BlockState state, int fortune, RandomSource rand) {
        return 2 + rand.nextInt(3) + fortune;
    }

    public static int base2Rand2Fortune(BlockState state, int fortune, RandomSource rand) {
        return 2 + rand.nextInt(2) + fortune;
    }

    public static int base1Rand2Fortune(BlockState state, int fortune, RandomSource rand) {
        return 1 + rand.nextInt(2) + fortune;
    }

    public static int base1Rand3(BlockState state, int fortune, RandomSource rand) {
        return 1 + rand.nextInt(3);
    }

    public static int const1(BlockState state, int fortune, RandomSource rand) {
        return 1;
    }

    public static int vanillaFortune(BlockState state, int fortune, RandomSource rand) {
        return 1 + applyFortune(rand, fortune);
    }

    public static int cobaltAmount(BlockState state, int fortune, RandomSource rand) {
        return 4 + rand.nextInt(6);
    }

    public static int alexandriteAmount(BlockState state, int fortune, RandomSource rand) {
        return Math.min(1 + rand.nextInt(2) + fortune, 2);
    }

    public static int cobaltNetherAmount(BlockState state, int fortune, RandomSource rand) {
        return 5 + rand.nextInt(8);
    }

    public static int applyFortune(RandomSource rand, int fortune) {
        return fortune <= 0 ? 0 : rand.nextInt(fortune);
    }
}
