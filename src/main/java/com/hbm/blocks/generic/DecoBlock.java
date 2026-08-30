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
 * Generic steel building-part deco block, ported from CE's {@code DecoBlock}. CE shares one class
 * across five registry entries ({@code steel_wall}, {@code steel_corner}, {@code steel_roof},
 * {@code steel_beam}, and satellite-dish props like {@code deco_sat_mapper}) and dispatches their
 * distinct hitboxes via {@code this == ModBlocks.steel_x} identity checks; the port flattens that
 * into an explicit {@link Shape} passed to the constructor, one instance per CE registry entry.
 * <p>
 * CE's {@code INBTBlockTransformable} structure-rotation hook and the empty {@code TileEntityDecoBlock}
 * (a render-distance-only override with no persisted state) are both dropped: the former has no
 * structure-rotation system to call it yet, and the latter has no modern equivalent need since block
 * entity render distance is not a per-entity concern in 1.21's chunk-based rendering.
 */
public class DecoBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final float F = 0.0625F;
    private static final VoxelShape WALL_WEST = Block.box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape WALL_EAST = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape ROOF = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape BEAM = Block.box(7, 0, 7, 9, 16, 9);

    public enum Shape { WALL, CORNER, ROOF, BEAM, PLAIN }

    private final Shape shape;

    public DecoBlock(Properties properties, Shape shape) {
        super(properties);
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        Direction facing = state.getValue(FACING);
        return switch (shape) {
            case WALL -> switch (facing) {
                case WEST -> WALL_WEST;
                case NORTH -> WALL_NORTH;
                case EAST -> WALL_EAST;
                case SOUTH -> WALL_SOUTH;
                default -> Shapes.block();
            };
            case CORNER -> switch (facing) {
                case EAST -> Shapes.or(WALL_EAST, WALL_SOUTH);
                case NORTH -> Shapes.or(WALL_NORTH, WALL_EAST);
                case SOUTH -> Shapes.or(WALL_SOUTH, WALL_WEST);
                case WEST -> Shapes.or(WALL_WEST, WALL_NORTH);
                default -> Shapes.block();
            };
            case ROOF -> ROOF;
            case BEAM -> BEAM;
            case PLAIN -> Shapes.block();
        };
    }
}
