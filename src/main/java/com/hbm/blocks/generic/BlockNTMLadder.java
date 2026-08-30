package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Modded ladder, ported from CE's {@code BlockNTMLadder}. Every registered variant behaves exactly
 * like vanilla's own {@link LadderBlock} except one ({@code ladder_red_top}, CE's short decorative
 * cap piece): it never requires a supporting wall and its outline is capped to a quarter block
 * tall. That single behavioral fork survives as a fixed-per-instance {@code capTop} flag - the same
 * pattern already used for {@link BlockRailing}'s {@code Kind} - rather than a full class per
 * variant, since eleven of the twelve CE registrations are otherwise identical to vanilla.
 */
public class BlockNTMLadder extends LadderBlock {

    private static final double CAP_HEIGHT = 4.0D;

    private final boolean capTop;

    public BlockNTMLadder(Properties properties) {
        this(properties, false);
    }

    public BlockNTMLadder(Properties properties, boolean capTop) {
        super(properties);
        this.capTop = capTop;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return capTop || super.canSurvive(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape full = super.getShape(state, level, pos, context);
        if (!capTop) {
            return full;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(0, 0, 0, 16, CAP_HEIGHT, 3);
            case WEST -> Block.box(13, 0, 0, 16, CAP_HEIGHT, 16);
            case EAST -> Block.box(0, 0, 0, 3, CAP_HEIGHT, 16);
            default -> Block.box(0, 0, 13, 16, CAP_HEIGHT, 16);
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!capTop) {
            return super.getStateForPlacement(context);
        }
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing);
    }
}
