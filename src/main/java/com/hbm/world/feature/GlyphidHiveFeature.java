package com.hbm.world.feature;

import com.hbm.blocks.generic.BlockGlyphid;
import com.hbm.blocks.generic.BlockGlyphidSpawner;
import com.hbm.blocks.generic.BlockLoot;
import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsPile;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE {@code com.hbm.world.feature.GlyphidHive} ({@code GlyphidHive.java}:17-170).
 * <p>
 * Gates: {@code enableDungeons} + {@code enableHives} + overworld only
 * ({@code HbmWorldGen.java}:347). Chance {@code MobConfig.hiveSpawn} default <b>256</b>.
 * {@code y = getTopSolidOrLiquidBlock + 1}, then {@code k=3..-1} first {@code isNormalCube}
 * ({@code :350-357}). 1/10 infected. Worldgen always loot=true. Schematic 11×5×11
 * ({@code schematicSmall}). FEATURES write-radius 0 clips overflow cells.
 */
public class GlyphidHiveFeature extends Feature<NoneFeatureConfiguration> {

    /** CE {@code GlyphidHive.schematicSmall} layers [y][x][z], y=0 is the top. */
    private static final int[][][] SCHEMATIC = GlyphidHiveSchematic.SMALL;

    public GlyphidHiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get() || !MobConfig.ENABLE_HIVES.get()) return false;

        WorldGenLevel level = context.level();
        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        if (!dimension.equals(Level.OVERWORLD)) return false;

        RandomSource random = context.random();
        int rate = MobConfig.HIVE_SPAWN.get();
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        BlockPos origin = context.origin();
        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int y = surface + 1;
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;

        BlockPos spawn = null;
        for (int k = 3; k >= -1; k--) {
            BlockPos check = new BlockPos(x, y - 1 + k, z);
            if (!level.hasChunk(check.getX() >> 4, check.getZ() >> 4)) continue;
            BlockState state = level.getBlockState(check);
            if (state.isCollisionShapeFullBlock(level, check)) {
                spawn = new BlockPos(x, y + k, z);
                break;
            }
        }
        if (spawn == null) return false;
        if (!CeStructureSpawn.locationIsValidSpawn(level, spawn, false)) return false;

        boolean infected = random.nextInt(10) == 0;
        placeHive(level, spawn, random, infected, true);
        return true;
    }

    private static void placeHive(WorldGenLevel level, BlockPos origin, RandomSource random, boolean infected, boolean loot) {
        BlockState base = PlantBlocks.glyphid(infected ? BlockGlyphid.Type.INFESTED : BlockGlyphid.Type.BASE)
                .get().defaultBlockState();
        BlockState spawner = PlantBlocks.GLYPHID_SPAWNER.get().defaultBlockState()
                .setValue(BlockGlyphidSpawner.TYPE, infected ? BlockGlyphidSpawner.Type.INFESTED : BlockGlyphidSpawner.Type.BASE);

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {
                    int block = SCHEMATIC[4 - j][i][k];
                    BlockPos pos = origin.offset(i - 5, j - 2, k - 5);
                    switch (block) {
                        case 1 -> CeSchematicPlacer.setBlockInRegion(level, pos, base);
                        case 2 -> {
                            if (random.nextInt(3) == 0) {
                                CeSchematicPlacer.setBlockInRegion(level, pos, spawner);
                            } else {
                                CeSchematicPlacer.setBlockInRegion(level, pos, base);
                            }
                        }
                        case 3 -> {
                            int r = random.nextInt(3);
                            if (r == 0) {
                                CeSchematicPlacer.setBlockInRegion(level, pos, Blocks.WITHER_SKELETON_SKULL.defaultBlockState()
                                        .setValue(SkullBlock.ROTATION, random.nextInt(16)));
                            } else if (r == 1) {
                                placePile(level, pos, random, ItemPoolsPile.POOL_PILE_BONES);
                            } else if (loot) {
                                placePile(level, pos, random, ItemPoolsPile.POOL_PILE_HIVE);
                            } else {
                                CeSchematicPlacer.setBlockInRegion(level, pos, base);
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }

    private static void placePile(WorldGenLevel level, BlockPos pos, RandomSource random, String poolName) {
        net.minecraft.world.level.block.Block loot = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(net.minecraft.resources.ResourceLocation.parse("hbm:deco_loot")).orElse(null);
        if (loot == null) return;
        CeSchematicPlacer.setBlockInRegion(level, pos, loot.defaultBlockState());
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) be = level.getLevel().getBlockEntity(pos);
        if (be instanceof BlockLoot.LootBlockEntity pile) {
            int limit = random.nextInt(3) + 3;
            ItemPool pool = ItemPool.getPool(poolName);
            for (int i = 0; i < limit; i++) {
                pile.addItem(ItemPool.getStack(pool, random));
            }
        }
    }
}
