package com.hbm.blocks.generic;

import com.hbm.blocks.PlantEnums.EnumDeadPlantType;
import com.hbm.blocks.PlantEnums.EnumFlowerPlantType;
import com.hbm.blocks.PlantEnums.EnumTallPlantType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockTallPlant}: the double-tall (lower/upper) half of the hemp and
 * mustard-willow growth chain started by {@link BlockNTMFlower}. CE modelled every stage as a
 * metadata value of one {@code BlockPlantEnumMeta<EnumTallPlantType>} block; each
 * {@link EnumTallPlantType} constant is its own registered block here (see {@link PlantBlocks}),
 * paired lower/upper lookups going through {@link PlantBlocks#tallPlant} instead of metadata
 * arithmetic. The lower/upper relationship itself follows vanilla's own
 * {@code DoublePlantBlock} idiom (paired-half {@code canSurvive}/{@code updateShape} checks)
 * rather than CE's {@code neighborChanged} string-suffix checks, since that idiom is what modern
 * Minecraft already uses for exactly this shape of block.
 *
 * <p>Deliberately not ported: CE's {@code getStateForPlacement} override, which force-corrected a
 * manually-placed upper-half item back to its lower half. With metadata gone, the upper-half
 * variants have their own {@code BlockItem} but are never added to a creative tab (see
 * {@link PlantBlocks}) and are otherwise only ever placed programmatically by this class, so that
 * correction has nothing left to guard against in normal play.
 */
public class BlockTallPlant extends BushBlock implements BonemealableBlock {

    public static final MapCodec<BlockTallPlant> CODEC =
            simpleCodec(properties -> new BlockTallPlant(properties, EnumTallPlantType.HEMP_LOWER));

    public final EnumTallPlantType type;

    public BlockTallPlant(Properties properties, EnumTallPlantType type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends BlockTallPlant> codec() {
        return CODEC;
    }

    private boolean isLower() {
        return type.name().endsWith("_LOWER");
    }

    private EnumTallPlantType pairedType() {
        String otherName = isLower() ? type.name().replace("_LOWER", "_UPPER") : type.name().replace("_UPPER", "_LOWER");
        return EnumTallPlantType.valueOf(otherName);
    }

    private EnumFlowerPlantType seedType() {
        return type == EnumTallPlantType.HEMP_LOWER || type == EnumTallPlantType.HEMP_UPPER
                ? EnumFlowerPlantType.HEMP
                : EnumFlowerPlantType.MUSTARD_WILLOW_0;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PlantBlocks.isTallPlantGround(state.getBlock());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!isLower()) {
            BlockState belowState = level.getBlockState(pos.below());
            return belowState.getBlock() == PlantBlocks.tallPlant(pairedType());
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isLower() && direction == Direction.UP && neighborState.getBlock() != PlantBlocks.tallPlant(pairedType())) {
            return PlantBlocks.flowerPlant(seedType()).defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (isLower()) {
            level.setBlock(pos.above(), PlantBlocks.tallPlant(pairedType()).defaultBlockState(), 2);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return isLower();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block ground = level.getBlockState(pos.below()).getBlock();
        if (!type.needsOil && PlantBlocks.isOiledOrDeadDirt(ground)) {
            level.setBlock(pos, PlantBlocks.deadPlant(EnumDeadPlantType.BIG_FLOWER).defaultBlockState(), 3);
            return;
        }
        if (isValidBonemealTarget(level, pos, state) && isBonemealSuccess(level, random, pos, state) && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return switch (type) {
            case MUSTARD_WILLOW_2_LOWER -> PlantBlocks.isWatered(level, pos);
            case MUSTARD_WILLOW_3_LOWER -> PlantBlocks.isWatered(level, pos) && (!type.needsOil || PlantBlocks.isOiled(level, pos));
            default -> false;
        };
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        if (type == EnumTallPlantType.MUSTARD_WILLOW_3_LOWER) {
            return true;
        }
        return random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        switch (type) {
            case MUSTARD_WILLOW_2_LOWER -> {
                level.setBlock(pos, PlantBlocks.tallPlant(EnumTallPlantType.MUSTARD_WILLOW_3_LOWER).defaultBlockState(), 2);
                level.setBlock(pos.above(), PlantBlocks.tallPlant(EnumTallPlantType.MUSTARD_WILLOW_3_UPPER).defaultBlockState(), 2);
            }
            case MUSTARD_WILLOW_3_LOWER -> {
                level.setBlock(pos, PlantBlocks.tallPlant(EnumTallPlantType.MUSTARD_WILLOW_4_LOWER).defaultBlockState(), 2);
                level.setBlock(pos.above(), PlantBlocks.tallPlant(EnumTallPlantType.MUSTARD_WILLOW_4_UPPER).defaultBlockState(), 2);
                level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            }
            default -> {
            }
        }
    }
}
