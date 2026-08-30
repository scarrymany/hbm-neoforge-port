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
 * Decorative satellite-dish pole, ported from CE's {@code DecoPoleSatelliteReceiver}. CE's
 * accompanying {@code TileEntityDecoPoleSatelliteReceiver} is empty apart from a render-distance
 * override with no modern equivalent need, so it is dropped rather than ported (see
 * {@link DecoBlock}'s class doc for the same reasoning).
 */
public class DecoPoleSatelliteReceiver extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public DecoPoleSatelliteReceiver(Properties properties) {
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
