package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative hanging/wall chain, ported from CE's {@code BlockChain}. CE's climbability came from
 * a {@code Block.isLadder(...)} override; that hook no longer exists on 1.21's
 * {@code BlockBehaviour} - modern climbing is entirely tag-driven ({@code minecraft:climbable},
 * confirmed via the decompiled {@code LadderBlock}/{@code BlockTags} sources for this exact
 * toolchain). This class ports the shape/support/placement logic faithfully; making the block
 * actually climbable is a one-line datapack follow-up (add this registry name to a
 * {@code climbable} block tag), not a Java concern, and is flagged in the port report rather than
 * guessed at here.
 */
public class BlockChain extends Block {

    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    public static final BooleanProperty END = BooleanProperty.create("end");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final float THICKNESS = 2.0F;
    private static final VoxelShape HANGING_SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BlockChain(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WALL, false).setValue(END, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WALL, END, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WALL) ? wallShape(state.getValue(FACING)) : HANGING_SHAPE;
    }

    private static VoxelShape wallShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(6, 0, 16 - THICKNESS, 10, 16, 16);
            case WEST -> Block.box(0, 0, 6, THICKNESS, 16, 10);
            case EAST -> Block.box(16 - THICKNESS, 0, 6, 16, 16, 10);
            default -> Block.box(6, 0, 0, 10, 16, THICKNESS);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos);
    }

    private static boolean hasSupport(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN) || level.getBlockState(above).getBlock() instanceof BlockChain) {
            return true;
        }
        return level.getBlockState(pos.west()).isFaceSturdy(level, pos.west(), Direction.EAST)
                || level.getBlockState(pos.east()).isFaceSturdy(level, pos.east(), Direction.WEST)
                || level.getBlockState(pos.north()).isFaceSturdy(level, pos.north(), Direction.SOUTH)
                || level.getBlockState(pos.south()).isFaceSturdy(level, pos.south(), Direction.NORTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();

        Direction wallFacing = null;
        if (clicked == Direction.SOUTH && level.getBlockState(pos.south()).isFaceSturdy(level, pos.south(), Direction.NORTH)) {
            wallFacing = Direction.SOUTH;
        } else if (clicked == Direction.NORTH && level.getBlockState(pos.north()).isFaceSturdy(level, pos.north(), Direction.SOUTH)) {
            wallFacing = Direction.NORTH;
        } else if (clicked == Direction.EAST && level.getBlockState(pos.east()).isFaceSturdy(level, pos.east(), Direction.WEST)) {
            wallFacing = Direction.EAST;
        } else if (clicked == Direction.WEST && level.getBlockState(pos.west()).isFaceSturdy(level, pos.west(), Direction.EAST)) {
            wallFacing = Direction.WEST;
        }

        boolean wall = wallFacing != null;
        BlockState below = level.getBlockState(pos.below());
        boolean linkedBelow = below.getBlock() instanceof BlockChain && below.getValue(WALL) == wall;
        boolean end = !linkedBelow && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);

        if (!wall) {
            BlockState above = level.getBlockState(pos.above());
            if (above.getBlock() instanceof BlockChain) {
                return this.defaultBlockState().setValue(FACING, above.getValue(FACING)).setValue(WALL, above.getValue(WALL)).setValue(END, end);
            }
        }

        return this.defaultBlockState().setValue(FACING, wall ? wallFacing : Direction.NORTH).setValue(WALL, wall).setValue(END, end);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!hasSupport(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        boolean wall = state.getValue(WALL);
        BlockState below = level.getBlockState(pos.below());
        boolean linkedBelow = below.getBlock() instanceof BlockChain && below.getValue(WALL) == wall;
        boolean end = !linkedBelow && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        return state.getValue(END) == end ? state : state.setValue(END, end);
    }
}
