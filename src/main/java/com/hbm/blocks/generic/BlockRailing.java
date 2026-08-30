package com.hbm.blocks.generic;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Decorative railing, ported from CE's {@code BlockRailing}. CE's constructor-time {@code type}
 * field (0/1: a thin wall-mounted panel against the face the player was looking away from, 2: two
 * such panels meeting at a corner) is not a block-state content variant - it never changes after
 * placement and is not exposed on the item, so each CE {@code type} stays a distinct Java-level
 * behavior selected once per registered instance, matching how CE itself registers a separate
 * named block per (type, texture) pair rather than folding {@code type} into item metadata.
 */
public class BlockRailing extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final double PANEL_THICKNESS = 2.0D;

    public enum Kind { PANEL, DOUBLE_PANEL }

    private final Kind kind;
    private final Map<Direction, VoxelShape> shapes;

    public BlockRailing(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.shapes = buildShapes(kind);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static Map<Direction, VoxelShape> buildShapes(Kind kind) {
        return Util.make(new EnumMap<>(Direction.class), map -> {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                VoxelShape near = panelAgainst(facing);
                if (kind == Kind.DOUBLE_PANEL) {
                    VoxelShape adjacent = panelAgainst(facing.getClockWise());
                    map.put(facing, Shapes.joinUnoptimized(near, adjacent, BooleanOp.OR));
                } else {
                    map.put(facing, near);
                }
            }
        });
    }

    /** A thin vertical panel flush against the wall the block is facing towards. */
    private static VoxelShape panelAgainst(Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(0, 0, 0, 16, 16, PANEL_THICKNESS);
            case SOUTH -> Block.box(0, 0, 16 - PANEL_THICKNESS, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, PANEL_THICKNESS, 16, 16);
            default -> Block.box(16 - PANEL_THICKNESS, 0, 0, 16, 16, 16);
        };
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public Kind getKind() {
        return kind;
    }
}
