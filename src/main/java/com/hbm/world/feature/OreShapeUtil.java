package com.hbm.world.feature;

import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Shared placement math and lookup helpers for every {@code com.hbm.world.feature} ore/deposit
 * {@link net.minecraft.world.level.levelgen.feature.Feature} added by this package, per
 * docs/phase4/ore_veins_and_bedrock_ores.md's "fork A" key design decision: one thin custom
 * {@code Feature<NoneFeatureConfiguration>} per shape family, each reading its own live
 * {@code CompatibilityConfig}/{@code WorldConfig}/{@code GeneralConfig} accessor inside
 * {@code place()} instead of baking a spawn rate into datapack JSON at datagen time (mirrors
 * neo-edition's own confirmed-real {@code OilBubbleFeature.place()} pattern, per the sibling
 * {@code worldgen_oil_and_meteor_dungeons.md} report).
 */
public final class OreShapeUtil {

    private OreShapeUtil() {
    }

    /**
     * Looks up an already-registered {@code hbm:<path>} block. Returns {@code null} if absent.
     * <p>
     * Deliberately resolved by name, fresh, every call - never cached in a static field at
     * class-load time. Every ore/target block this package's {@code Feature} classes reference is
     * looked up this way rather than through a compile-time {@code DeferredBlock}/
     * {@code DeferredHolder} field, because {@code Feature} instances are constructed by their
     * {@code DeferredRegister<Feature<?>>} supplier at mod-construction time, potentially before
     * {@code ModBlocks}' own {@code RegisterEvent} has fired - the exact "eager {@code .get()} on a
     * DeferredHolder crashes with IllegalStateException" pattern this port's own conventions warn
     * against. Resolving by name inside {@code place()} (always long after every registry has
     * closed, since world generation cannot start before mod setup finishes) sidesteps that
     * ordering hazard entirely.
     */
    @Nullable
    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * {@code WorldGenLevel} does not itself expose the owning dimension or world seed - both live
     * on the underlying {@link ServerLevel}, reachable via {@code WorldGenLevel#getLevel()}. This is
     * well-established vanilla feature-placement API (used internally by several vanilla
     * {@code Feature} implementations to reach seed/dimension/structure-manager state a bare
     * {@code WorldGenLevel} doesn't provide) but, per this task's own ground rules, is not
     * independently verified against a compiled NeoForge 1.21.1 jar in this sandbox - centralized
     * here so a future agent only has one call site to fix if it turns out wrong.
     */
    public static ServerLevel serverLevel(WorldGenLevel level) {
        return level.getLevel();
    }

    public static ResourceKey<Level> dimension(WorldGenLevel level) {
        return serverLevel(level).dimension();
    }

    public static long seed(WorldGenLevel level) {
        return serverLevel(level).getSeed();
    }

    /** Floors {@code coord} to the start of its containing 16-block chunk column. */
    public static int chunkOrigin(int coord) {
        return coord & ~15;
    }

    /**
     * Ported from CE's {@code WorldGenMinableNonCascade.postGenerate} (upstream hbm-ce, 173 lines) -
     * the near-verbatim-vanilla ellipsoid-blob ore-vein shape every ordinary CE vein (Group A/B/C of
     * the research report) uses. CE's two independently-computed but byte-identical {@code d10}/
     * {@code d11} radius terms are collapsed to one local variable here (dead duplication in the
     * original, confirmed by reading both formulas are the same expression - not a behavior change).
     * Only overwrites a block when it currently matches {@code target} - CE's own
     * {@code isReplaceableOreGen(state, world, pos, BlockMatcher.forBlock(target))}, which for every
     * vanilla block reduces to an exact same-block identity check, ported here as
     * {@code BlockState#is(Block)}.
     *
     * @return true if at least one block was actually placed.
     */
    public static boolean placeEllipsoidVein(WorldGenLevel level, RandomSource random, int x, int y, int z,
                                              int numberOfBlocks, BlockState oreState, Block target) {
        if (numberOfBlocks <= 0) return false;

        float f = random.nextFloat() * (float) Math.PI;
        double d0 = x + 8.0 + Mth.sin(f) * numberOfBlocks / 8.0F;
        double d1 = x + 8.0 - Mth.sin(f) * numberOfBlocks / 8.0F;
        double d2 = z + 8.0 + Mth.cos(f) * numberOfBlocks / 8.0F;
        double d3 = z + 8.0 - Mth.cos(f) * numberOfBlocks / 8.0F;
        double d4 = y + random.nextInt(3) - 2;
        double d5 = y + random.nextInt(3) - 2;

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int i = 0; i < numberOfBlocks; ++i) {
            float f1 = (float) i / (float) numberOfBlocks;
            double d6 = d0 + (d1 - d0) * f1;
            double d7 = d4 + (d5 - d4) * f1;
            double d8 = d2 + (d3 - d2) * f1;
            double d9 = random.nextDouble() * numberOfBlocks / 16.0D;
            double d10 = (Mth.sin((float) Math.PI * f1) + 1.0F) * d9 + 1.0D;

            int j = Mth.floor(d6 - d10 / 2.0D);
            int k = Mth.floor(d7 - d10 / 2.0D);
            int l = Mth.floor(d8 - d10 / 2.0D);
            int i1 = Mth.floor(d6 + d10 / 2.0D);
            int j1 = Mth.floor(d7 + d10 / 2.0D);
            int k1 = Mth.floor(d8 + d10 / 2.0D);

            for (int l1 = j; l1 <= i1; ++l1) {
                double d12 = ((double) l1 + 0.5D - d6) / (d10 / 2.0D);
                if (d12 * d12 >= 1.0D) continue;

                int kLo = Math.max(k, minY);
                int j1Hi = Math.min(j1, maxY - 1);
                for (int i2 = kLo; i2 <= j1Hi; ++i2) {
                    double d13 = ((double) i2 + 0.5D - d7) / (d10 / 2.0D);
                    if (d12 * d12 + d13 * d13 >= 1.0D) continue;

                    for (int j2 = l; j2 <= k1; ++j2) {
                        double d14 = ((double) j2 + 0.5D - d8) / (d10 / 2.0D);
                        if (d12 * d12 + d13 * d13 + d14 * d14 >= 1.0D) continue;

                        pos.set(l1, i2, j2);
                        if (level.getBlockState(pos).is(target)) {
                            level.setBlock(pos, oreState, 2 | 16);
                            placedAny = true;
                        }
                    }
                }
            }
        }
        return placedAny;
    }
}
