package com.hbm.explosion;

import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.handler.ArmorUtil;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Exact CE {@code com.hbm.explosion.ExplosionThermo} (381 lines). 1.21 flatten:
 * {@code GRASS}→{@code GRASS_BLOCK}, {@code STONEBRICK}→{@code STONE_BRICKS},
 * {@code SNOW}/{@code SNOW_LAYER}→{@code SNOW_BLOCK}/{@code SNOW}, flowing lava/water are
 * {@code LAVA}/{@code WATER}. Log/plank/leaf identity is {@link BlockTags}. Clay→terracotta
 * (CE stained-clay meta). {@code isWarDim} stub true (same as {@link ExplosionChaos}).
 */
public final class ExplosionThermo {

    private static final Block[] TERRACOTTA = {
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
            Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA,
            Blocks.BLACK_TERRACOTTA
    };

    private ExplosionThermo() {
    }

    public static void freeze(Level level, Entity detonator, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = bombStartStrength * 2;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + level.getRandom().nextInt(r22 / 2))
                        pos.set(X, Y, Z);
                    freezeDest(level, pos);
                }
            }
        }
    }

    public static void scorch(Level level, Entity detonator, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = bombStartStrength * 2;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + level.getRandom().nextInt(r22 / 2))
                        pos.set(X, Y, Z);
                    scorchDest(level, pos);
                }
            }
        }
    }

    public static void scorchDest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == PlantBlocks.FROZEN_GRASS.get()) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == Blocks.DIRT) {
            level.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), 3);
        } else if (block == ours("frozen_dirt")) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == Blocks.NETHERRACK) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } else if (block == PlantBlocks.FROZEN_LOG.get()) {
            level.setBlock(pos, copyAxis(state, PlantBlocks.WASTE_LOG.get().defaultBlockState()), 3);
        } else if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, copyAxis(state, PlantBlocks.WASTE_LOG.get().defaultBlockState()), 3);
        } else if (block == ours("frozen_planks")) {
            level.setBlock(pos, ours("waste_planks").defaultBlockState(), 3);
        } else if (state.is(BlockTags.PLANKS)) {
            level.setBlock(pos, ours("waste_planks").defaultBlockState(), 3);
        } else if (block == Blocks.STONE) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } else if (block == Blocks.COBBLESTONE) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } else if (block == Blocks.STONE_BRICKS) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } else if (block == Blocks.OBSIDIAN) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } else if (state.is(BlockTags.LEAVES)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.WATER) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.PACKED_ICE) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        } else if (block == Blocks.ICE) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.SNOW_BLOCK) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.SNOW) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static void freezeDest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK) {
            level.setBlock(pos, PlantBlocks.FROZEN_GRASS.get().defaultBlockState(), 3);
        } else if (block == Blocks.DIRT) {
            level.setBlock(pos, ours("frozen_dirt").defaultBlockState(), 3);
        } else if (state.is(BlockTags.PLANKS) && block != ours("waste_planks")) {
            level.setBlock(pos, ours("frozen_planks").defaultBlockState(), 3);
        } else if (block == PlantBlocks.WASTE_LOG.get()) {
            level.setBlock(pos, copyAxis(state, PlantBlocks.FROZEN_LOG.get().defaultBlockState()), 3);
        } else if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, copyAxis(state, PlantBlocks.FROZEN_LOG.get().defaultBlockState()), 3);
        } else if (block == ours("waste_planks")) {
            level.setBlock(pos, ours("frozen_planks").defaultBlockState(), 3);
        } else if (block == Blocks.STONE) {
            level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
        } else if (block == Blocks.COBBLESTONE) {
            level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
        } else if (block == Blocks.STONE_BRICKS) {
            level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
        } else if (state.is(BlockTags.LEAVES)) {
            level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
        } else if (block == Blocks.LAVA) {
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        } else if (block == Blocks.WATER) {
            level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
        }
    }

    public static void freezer(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        float f = bombStartStrength;
        double wat = bombStartStrength;

        bombStartStrength *= 2.0F;
        int i = Mth.floor(x - wat - 1.0D);
        int j = Mth.floor(x + wat + 1.0D);
        int k = Mth.floor(y - wat - 1.0D);
        int i2 = Mth.floor(y + wat + 1.0D);
        int l = Mth.floor(z - wat - 1.0D);
        int j2 = Mth.floor(z + wat + 1.0D);
        List<Entity> list = level.getEntities((Entity) null, new AABB(i, k, l, j, i2, j2));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Entity entity : list) {
            double d4 = entity.position().distanceTo(new Vec3(x, y, z)) / bombStartStrength;

            if (d4 <= 1.0D) {
                double d5 = entity.getX() - x;
                double d6 = entity.getY() + entity.getEyeHeight() - y;
                double d7 = entity.getZ() - z;
                double d9 = Mth.sqrt((float) (d5 * d5 + d6 * d6 + d7 * d7));
                if (d9 < wat && !(entity instanceof Ocelot) && entity instanceof LivingEntity living) {
                    for (int a = (int) entity.getX() - 2; a < (int) entity.getX() + 1; a++) {
                        for (int b = (int) entity.getY(); b < (int) entity.getY() + 3; b++) {
                            for (int c = (int) entity.getZ() - 1; c < (int) entity.getZ() + 2; c++) {
                                pos.set(a, b, c);
                                level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                            }
                        }
                    }

                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90 * 20, 2));
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3 * 60 * 20, 2));
                }
            }
        }

        bombStartStrength = (int) f;
    }

    public static void setEntitiesOnFire(Level level, double x, double y, double z, int radius) {
        if (!isWarDim(level)) return;
        List<Entity> list = level.getEntities((Entity) null,
                new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));

        for (Entity e : list) {
            if (e.position().distanceTo(new Vec3(x, y, z)) <= radius) {
                if (!(e instanceof Player player && ArmorUtil.checkForAsbestos(player))) {
                    if (e instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 4));
                    }
                    e.igniteForSeconds(10);
                }
            }
        }
    }

    public static void scorchLight(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = bombStartStrength * 2;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + level.getRandom().nextInt(r22 / 2))
                        scorchDestLight(level, pos.set(X, Y, Z));
                }
            }
        }
    }

    public static void scorchDestLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == PlantBlocks.FROZEN_GRASS.get()) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == Blocks.DIRT) {
            level.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), 3);
        } else if (block == ours("frozen_dirt")) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == PlantBlocks.WASTE_EARTH.get()) {
            level.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), 3);
        } else if (block == PlantBlocks.FROZEN_LOG.get()) {
            level.setBlock(pos, copyAxis(state, PlantBlocks.WASTE_LOG.get().defaultBlockState()), 3);
        } else if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, copyAxis(state, PlantBlocks.WASTE_LOG.get().defaultBlockState()), 3);
        } else if (block == ours("frozen_planks")) {
            level.setBlock(pos, ours("waste_planks").defaultBlockState(), 3);
        } else if (state.is(BlockTags.PLANKS)) {
            level.setBlock(pos, ours("waste_planks").defaultBlockState(), 3);
        } else if (block == Blocks.OBSIDIAN) {
            level.setBlock(pos, ours("gravel_obsidian").defaultBlockState(), 3);
        } else if (state.is(BlockTags.LEAVES)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.WATER) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.PACKED_ICE) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        } else if (block == Blocks.ICE) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.SAND) {
            level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3);
        } else if (block == Blocks.CLAY) {
            level.setBlock(pos, TERRACOTTA[level.getRandom().nextInt(16)].defaultBlockState(), 3);
        }
    }

    public static void snow(Level level, Entity detonator, int x, int y, int z, int bound) {
        if (!isWarDim(level)) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int r = bound;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        pos.set(X, Y + 1, Z);
                        BlockState here = level.getBlockState(pos);
                        BlockState layer = Blocks.SNOW.defaultBlockState();
                        if (layer.canSurvive(level, pos) && (here.is(Blocks.AIR) || here.is(Blocks.FIRE))) {
                            level.setBlock(pos, layer, 3);
                        }
                    }
                }
            }
        }
    }

    private static BlockState copyAxis(BlockState from, BlockState to) {
        if (from.hasProperty(RotatedPillarBlock.AXIS) && to.hasProperty(RotatedPillarBlock.AXIS)) {
            return to.setValue(RotatedPillarBlock.AXIS, from.getValue(RotatedPillarBlock.AXIS));
        }
        return to;
    }

    private static boolean isWarDim(Level level) {
        return true;
    }

    private static Block ours(String path) {
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(null);
        return block != null ? block : Blocks.AIR;
    }
}
