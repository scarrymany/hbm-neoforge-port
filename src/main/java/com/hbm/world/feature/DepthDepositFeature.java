package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Group D (depth-ore family), ported from CE's {@code DepthDeposit} (168 lines, read in full) - a
 * solid sphere blob with a hollow shell, distinct from Group A/B/C's ellipsoid vein math. Every call
 * is gated {@code if (rand.nextInt(chance) != 0) return;} then samples one random point per
 * y-band; the nether variants ({@code ore_depth_nether_neodymium}/{@code _nitan}) roll <b>twice</b>,
 * once per y-band ({@code [0,3)} and {@code [125,128)}), each an independent chance roll - preserved
 * here as two entries in {@link #yMins} rather than collapsing to one roll, matching CE's own two
 * separate top-level {@code DepthDeposit.generateConditionNether} call sites exactly.
 * <p>
 * The replaceable check tests against <b>both</b> {@code Blocks.BEDROCK} and the caller's
 * {@code target} (CE's own comment: {@code //yes you've heard right, bedrock} - a depth deposit can
 * carve into isolated pre-existing bedrock the same as ordinary stone/netherrack).
 */
public class DepthDepositFeature extends Feature<NoneFeatureConfiguration> {

    private final int[] yMins;
    private final int yDev;
    private final int chance;
    private final int size;
    private final double fill;
    private final String oreBlockName;
    private final boolean nether;
    private final String fillerBlockName;

    public DepthDepositFeature(Codec<NoneFeatureConfiguration> codec, int[] yMins, int yDev, int chance, int size,
                                double fill, String oreBlockName, boolean nether, String fillerBlockName) {
        super(codec);
        this.yMins = yMins;
        this.yDev = yDev;
        this.chance = chance;
        this.size = size;
        this.fill = fill;
        this.oreBlockName = oreBlockName;
        this.nether = nether;
        this.fillerBlockName = fillerBlockName;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        Block ore = OreShapeUtil.block(oreBlockName);
        Block filler = OreShapeUtil.block(fillerBlockName);
        if (ore == null || filler == null) return false;
        Block target = nether ? Blocks.NETHERRACK : Blocks.STONE;
        BlockState oreState = ore.defaultBlockState();
        BlockState fillerState = filler.defaultBlockState();

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());

        boolean placedAny = false;
        for (int yMin : yMins) {
            if (random.nextInt(chance) != 0) continue;

            int cx = chunkMinX + random.nextInt(16);
            int cy = yMin + random.nextInt(yDev);
            int cz = chunkMinZ + random.nextInt(16);
            placedAny |= generateSphere(level, random, cx, cy, cz, size, fill, oreState, target, fillerState);
        }
        return placedAny;
    }

    /** Ported from CE's {@code DepthDeposit.generateSphere}. */
    private static boolean generateSphere(WorldGenLevel level, RandomSource random, int cx, int cy, int cz,
                                           int size, double fill, BlockState ore, Block target, BlockState filler) {
        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(1, level.getMinBuildHeight());
        int maxY = Math.min(126, level.getMaxBuildHeight() - 1);

        for (int ix = cx - size; ix <= cx + size; ix++) {
            int dx = ix - cx;
            for (int jy = Math.max(cy - size, minY); jy <= Math.min(cy + size, maxY); jy++) {
                int dy = jy - cy;
                for (int kz = cz - size; kz <= cz + size; kz++) {
                    int dz = kz - cz;

                    pos.set(ix, jy, kz);
                    BlockState state = level.getBlockState(pos);
                    boolean matches = state.is(Blocks.BEDROCK) || state.is(target);
                    if (!matches) continue;

                    double len = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
                    if (len + random.nextInt(2) < size * fill) {
                        level.setBlock(pos, ore, 2 | 16);
                        placedAny = true;
                    } else if (len + random.nextInt(2) <= size) {
                        level.setBlock(pos, filler, 2 | 16);
                    }
                }
            }
        }
        return placedAny;
    }
}
