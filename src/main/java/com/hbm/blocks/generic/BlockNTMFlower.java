package com.hbm.blocks.generic;

import com.hbm.blocks.PlantEnums.EnumDeadPlantType;
import com.hbm.blocks.PlantEnums.EnumFlowerPlantType;
import com.hbm.blocks.PlantEnums.EnumTallPlantType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockNTMFlower}: a {@code BlockPlantEnumMeta<EnumFlowerPlantType>} with
 * bonemeal-driven growth for the {@code HEMP}/{@code MUSTARD_WILLOW_0}/{@code MUSTARD_WILLOW_1}
 * variants (the rest are purely decorative). Each {@link EnumFlowerPlantType} constant is its own
 * registered block (see {@link PlantBlocks}); the hemp/mustard-willow growth chain that used to
 * pivot on a shared metadata value now pivots on cross-references between the distinct
 * {@code plant_flower_*}, {@code plant_tall_*} and {@code plant_dead_*} blocks in
 * {@link PlantBlocks}.
 *
 * <p>CE's fallback {@code grow()} branch (any other type) spawned a dropped-item entity of itself -
 * with modern loot tables handling drops, that branch is redundant and not ported.
 */
public class BlockNTMFlower extends BushBlock implements BonemealableBlock {

    public static final MapCodec<BlockNTMFlower> CODEC =
            simpleCodec(properties -> new BlockNTMFlower(properties, EnumFlowerPlantType.FOXGLOVE));

    public final EnumFlowerPlantType type;

    public BlockNTMFlower(Properties properties, EnumFlowerPlantType type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends BlockNTMFlower> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PlantBlocks.isFlowerPlantGround(state.getBlock());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return isGrowingType();
    }

    private boolean isGrowingType() {
        return type == EnumFlowerPlantType.HEMP
                || type == EnumFlowerPlantType.MUSTARD_WILLOW_0
                || type == EnumFlowerPlantType.MUSTARD_WILLOW_1;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isValidBonemealTarget(level, pos, state) && isBonemealSuccess(level, random, pos, state) && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!isGrowingType()) {
            return false;
        }
        if ((type == EnumFlowerPlantType.MUSTARD_WILLOW_0 || type == EnumFlowerPlantType.MUSTARD_WILLOW_1)
                && !PlantBlocks.isWatered(level, pos)) {
            return false;
        }
        if (type == EnumFlowerPlantType.HEMP || type == EnumFlowerPlantType.MUSTARD_WILLOW_1) {
            return level.getBlockState(pos.above()).isAir();
        }
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return switch (type) {
            case HEMP, MUSTARD_WILLOW_0, MUSTARD_WILLOW_1 -> random.nextFloat() < 0.33F;
            default -> true;
        };
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        switch (type) {
            case HEMP -> {
                Block ground = level.getBlockState(pos.below()).getBlock();
                if (PlantBlocks.isOiledOrDeadDirt(ground)) {
                    level.setBlock(pos, PlantBlocks.deadPlant(EnumDeadPlantType.GENERIC).defaultBlockState(), 3);
                    return;
                }
                level.setBlock(pos, PlantBlocks.tallPlant(EnumTallPlantType.HEMP_LOWER).defaultBlockState(), 2);
                level.setBlock(pos.above(), PlantBlocks.tallPlant(EnumTallPlantType.HEMP_UPPER).defaultBlockState(), 2);
            }
            case MUSTARD_WILLOW_0 -> {
                if (PlantBlocks.isWatered(level, pos)) {
                    level.setBlock(pos, PlantBlocks.flowerPlant(EnumFlowerPlantType.MUSTARD_WILLOW_1).defaultBlockState(), 3);
                }
            }
            case MUSTARD_WILLOW_1 -> {
                if (PlantBlocks.isWatered(level, pos)) {
                    level.setBlock(pos, PlantBlocks.tallPlant(EnumTallPlantType.MUSTARD_WILLOW_2_LOWER).defaultBlockState(), 3);
                    level.setBlock(pos.above(), PlantBlocks.tallPlant(EnumTallPlantType.MUSTARD_WILLOW_2_UPPER).defaultBlockState(), 3);
                }
            }
            default -> {
                // Purely decorative variants never receive random ticks (see isGrowingType()),
                // so performBonemeal is never reached for them.
            }
        }
    }
}
