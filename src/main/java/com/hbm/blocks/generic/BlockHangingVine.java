package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * Ported from CE's {@code BlockHangingVine}: a single-registry decorative vine ({@code
 * vine_phosphor}) whose visible "part" (ground/middle/hang) is derived from its surroundings. CE
 * computed this via {@code getActualState}, a Forge-1.12 mechanism with no modern equivalent;
 * 1.13+ folded that concept into ordinary blockstate properties recomputed through
 * {@code updateShape}, which is what this port uses instead.
 */
public class BlockHangingVine extends Block {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public BlockHangingVine(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, Part.HANG));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computePart(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return computePart(state, level, pos);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private BlockState computePart(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        if (belowState.isFaceSturdy(level, below, Direction.UP)) {
            return state.setValue(PART, Part.GROUND);
        }
        if (belowState.getBlock() == this) {
            return state.setValue(PART, Part.MIDDLE);
        }
        return state.setValue(PART, Part.HANG);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 0.5, 0.5));
        entity.resetFallDistance();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return aboveState.isFaceSturdy(level, above, Direction.DOWN) || aboveState.getBlock() == this;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathType) {
        return false;
    }

    public enum Part implements StringRepresentable {
        GROUND, MIDDLE, HANG;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
