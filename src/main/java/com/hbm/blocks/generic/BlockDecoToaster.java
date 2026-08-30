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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative toaster prop, ported from CE's {@code BlockDecoToaster} (via {@code BlockDecoModel}).
 * See {@link BlockDecoCRT} for the metadata-flattening rationale (one instance per material
 * {@link Variant}, rotation kept as a real {@code FACING} property) and the OBJ-model rendering gap.
 */
public class BlockDecoToaster extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_NS = Block.box(4, 0, 6, 12, 5.2, 10);
    private static final VoxelShape SHAPE_EW = Block.box(6, 0, 4, 10, 5.2, 12);

    public enum Variant { IRON, STEEL, WOOD }

    private final Variant variant;

    public BlockDecoToaster(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        return axis == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }
}
