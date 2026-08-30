package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

/**
 * Reactor-meltdown-themed layered decorative block (foam, molten sand, waste leaves, oil spill),
 * ported from CE's {@code BlockLayering}. CE's {@code BlockBakeBase} superclass only existed for
 * runtime cube/column model generation, dropped per the port's datagen ground rule (see
 * {@link BlockGenericPWR}'s javadoc for the same note). CE's support check also special-cased two
 * sibling decorative classes ({@code ZirnoxDestroyed}, {@code RBMKDebris}) that have not been
 * ported yet in this port tree; that specific carve-out is omitted here (flagged in the port
 * report) rather than guessed at - the general leaves/opaque-support/self-stacking checks are
 * ported faithfully and cover the overwhelming majority of real placement surfaces.
 */
public class BlockLayering extends Block {

    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 0, 7);
    private static final int MAX_LAYER = 7;

    public BlockLayering(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int layers = state.getValue(LAYERS) & MAX_LAYER;
        float height = (2 * (1 + layers)) / 16.0F;
        return Block.box(0, 0, 0, 16, height * 16, 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        Block belowBlock = below.getBlock();

        if (belowBlock == Blocks.ICE || belowBlock == Blocks.PACKED_ICE) {
            return false;
        }
        if (below.is(BlockTags.LEAVES)) {
            return true;
        }
        if (belowBlock == this && (below.getValue(LAYERS) & MAX_LAYER) == MAX_LAYER) {
            return true;
        }
        return below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }
}
