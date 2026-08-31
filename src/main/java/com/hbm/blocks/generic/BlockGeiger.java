package com.hbm.blocks.generic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * World-gen casing for CE {@code com.hbm.blocks.machine.GeigerCounter} ({@code ModBlocks.java}:857,
 * {@code FACING = BlockHorizontal.FACING} at {@code GeigerCounter.java}:28). Full TE / click-to-read
 * is Phase 2 machine work; Bunker ({@code Bunker.java}:277) only needs the facing block.
 */
public class BlockGeiger extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockGeiger(Properties properties) {
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
}
