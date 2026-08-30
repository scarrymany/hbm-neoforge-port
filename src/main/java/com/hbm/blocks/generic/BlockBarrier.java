package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible/thin barrier-style deco block, ported from CE's {@code BlockBarrier}. CE's dynamic
 * neighbor-connection rendering relies on {@code IExtendedBlockState}/{@code IUnlistedProperty},
 * which has no NeoForge 1.21 equivalent (same gap the research report flags for
 * {@code BlockSandbags}); this port keeps the real, gameplay-relevant part - a thin collision plane
 * on the side the player was facing when they placed it - and drops the per-neighbor visual
 * connection meshing as a documented rendering simplification (plain default model instead).
 */
public class BlockBarrier extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape POS_X = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape NEG_X = Block.box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape POS_Z = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape NEG_Z = Block.box(0, 0, 14, 16, 16, 16);

    public BlockBarrier(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> POS_X;
            case WEST -> NEG_X;
            case SOUTH -> POS_Z;
            case NORTH -> NEG_Z;
            default -> Shapes.block();
        };
    }
}
