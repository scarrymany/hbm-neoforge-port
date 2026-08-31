package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.world.OilBubble} (197 lines, full read), triggered in CE by
 * {@code com.hbm.lib.HbmWorldGen} lines 635-650 - the underground oil-deposit pocket plus its
 * cosmetic surface staining. See docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 1.
 * <p>
 * Per the research report's Key design decisions #3/#4 (confirmed real via neo-edition's own
 * compiling {@code OilBubbleFeature}), the {@code oilBubbleSpawn} 1-in-N roll happens live inside
 * {@link #place} against {@link CompatibilityConfig}'s (now real) dimension-keyed map rather than
 * being baked into a datapack-JSON {@code RarityFilter} - this preserves CE's server-operator-tunable
 * spawn rate. {@code OilMeteorPlacedFeatures} supplies exactly one candidate position per chunk
 * (random X/Z via {@code InSquarePlacement.spread()}, Y via
 * {@code HeightRangePlacement.uniform(0, 24)} matching CE's own {@code y = rand.nextInt(25)}); this
 * method never re-derives X/Y/Z itself, only whether/how big a bubble to stamp there.
 * <p>
 * <b>Hot/dry biome halving.</b> CE's exact condition is {@code getDefaultTemperature() >= 2.0F &&
 * getRainfall() < 0.1F}. No public rainfall/downfall accessor on {@code Biome} was confirmed reachable
 * in this sandbox (no compiled NeoForge jar available to verify against). This substitutes
 * {@code biome.getPrecipitationAt(pos) == Biome.Precipitation.NONE} for the rainfall half of that
 * check - exactly the substitution this report's own research already found and endorsed by reading
 * neo-edition's compiling {@code OilBubbleFeature.getSpawnRate}.
 * <p>
 * <b>Surface dressing.</b> The 150-column Gaussian dirt/sand/stone-staining scatter is delegated to
 * this port's already-real {@link OilSpot#generateOilSpot} ({@code width=7, count=150}, matching
 * CE's own {@code spotWidth}/{@code spotCount}) rather than re-deriving CE's separate (but similar)
 * {@code OilBubble.addSurfaceSpot} first loop. Only that method's second loop - the small oil-spill
 * puddle punched into one of 5 cardinal-adjacent columns from the bubble's own origin - has no
 * existing port equivalent and is ported directly below.
 */
public class OilBubbleFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_RADIUS = 10;
    private static final int RADIUS_VARIATION = 7;
    private static final int HOT_DRY_DIVISOR = 3;
    private static final int SPOT_WIDTH = 7;
    private static final int SPOT_COUNT = 150;

    public OilBubbleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int baseRate = CompatibilityConfig.forDimension(CompatibilityConfig.oilBubbleSpawn(), dimension);
        if (baseRate <= 0) return false;

        int spawnRate = baseRate;
        Biome biome = level.getBiome(origin).value();
        if (biome.getBaseTemperature() >= 2.0F && biome.getPrecipitationAt(origin) == Biome.Precipitation.NONE) {
            spawnRate = Math.max(1, spawnRate / HOT_DRY_DIVISOR);
        }
        if (random.nextInt(spawnRate) != 0) return false;

        Block oreOil = OreShapeUtil.block("ore_oil");
        if (oreOil == null) return false;

        int radius = MIN_RADIUS + random.nextInt(RADIUS_VARIATION);
        spawnOil(level, origin.getX(), origin.getY(), origin.getZ(), radius, oreOil);
        OilSpot.generateOilSpot(level, origin.getX(), origin.getZ(), SPOT_WIDTH, SPOT_COUNT, false);
        placeOilSpillPuddle(level, origin.getX(), origin.getZ());
        return true;
    }

    private static void spawnOil(WorldGenLevel level, int x, int y, int z, int radius, Block oreOil) {
        int r2 = radius * radius;
        int r22 = r2 / 2;
        BlockState oreState = oreOil.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int xx = -radius; xx < radius; xx++) {
            int xPos = xx + x;
            int xx2 = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int yPos = yy + y;
                int yy2 = xx2 + yy * yy * 3;
                for (int zz = -radius; zz < radius; zz++) {
                    int zPos = zz + z;
                    int distSq = yy2 + zz * zz;
                    if (distSq < r22) {
                        pos.set(xPos, yPos, zPos);
                        if (level.getBlockState(pos).is(Blocks.STONE)) {
                            level.setBlock(pos, oreState, 2 | 16);
                        }
                    }
                }
            }
        }
    }

    /** Ported from CE's {@code OilBubble.addSurfaceSpot}'s second loop only - see class javadoc. */
    private static void placeOilSpillPuddle(WorldGenLevel level, int xCoord, int zCoord) {
        Block stoneCracked = OreShapeUtil.block("stone_cracked");
        Block oilSpill = OreShapeUtil.block("oil_spill");
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int i = 1; i < 6; i++) {
            Direction facing = Direction.from3DDataValue(i);
            int x = xCoord + facing.getStepX();
            int z = zCoord + facing.getStepZ();
            int solids = 0;

            for (int y = maxY - 1; y >= minY; y--) {
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (!state.getFluidState().isEmpty()) break;

                if (state.isSolidRender(level, pos)) {
                    solids++;
                    if (i > 1) {
                        setIfPresent(level, pos, stoneCracked);
                        if (solids >= 4) break;
                    } else {
                        if (solids < 3) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                        if (solids == 3) setIfPresent(level, pos, oilSpill);
                        if (solids > 3 && solids < 7) setIfPresent(level, pos, stoneCracked);
                        if (solids == 7) break;
                    }
                }
            }
        }
    }

    private static void setIfPresent(WorldGenLevel level, BlockPos pos, @Nullable Block block) {
        if (block != null) level.setBlock(pos, block.defaultBlockState(), 2 | 16);
    }
}
