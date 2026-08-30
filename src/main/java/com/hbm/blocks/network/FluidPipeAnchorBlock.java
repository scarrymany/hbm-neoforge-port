package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.PipeAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Long-distance, wrench-linked fluid pipe anchor, ported from CE's
 * {@code com.hbm.blocks.network.FluidPipeAnchor}. {@link #FACING} is the one gameplay-relevant state
 * this family keeps beyond the connection-mask cache: {@link PipeAnchorBlockEntity#createNode} reads
 * it to determine the anchor's single normal (face-adjacent) connection direction (the side opposite
 * the way it's facing, matching CE's {@code ForgeDirection.getOrientation(getBlockMetadata()).getOpposite()}).
 * The custom placement-AABB ({@code IBlockSpecialPlacementAABB}) is deferred to Phase 5 with the rest
 * of this family's rendering/collision geometry - see {@link FluidDuctBaseBlock}'s javadoc.
 */
public class FluidPipeAnchorBlock extends FluidDuctBaseBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FluidPipeAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeAnchorBlockEntity(FluidDuctBlockEntities.ANCHOR_TYPE.get(), pos, state);
    }
}
