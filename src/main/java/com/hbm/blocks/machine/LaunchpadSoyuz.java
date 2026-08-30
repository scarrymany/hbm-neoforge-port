package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.LaunchInfraBlockEntities;
import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.machine.LaunchpadSoyuz} (130 lines, read in full) - CE's
 * most visually ambitious launch structure: a {@code BlockDummyable} multiblock whose
 * {@link #getAllDimensions()} returns 19 separate shape entries. {@link #checkRequirement}/
 * {@link #fillSpace} below are a direct, mechanical translation of CE's own 19 hand-written
 * {@code MultiblockHandlerXR.checkSpace}/{@code fillSpace} calls (each with its own hand-picked
 * pivot offset from the core) - not simplified or deduplicated, since CE itself doesn't loop over
 * {@link #getAllDimensions()} for this, it calls each shape out individually with its own pivot
 * math. {@code ForgeDirection.offsetX/offsetZ} become {@link Direction#getStepX()}/
 * {@link Direction#getStepZ()}; every {@code (x, y, z)} triple becomes a single pivot
 * {@link BlockPos}, computed once as {@code core} then offset per entry.
 */
public class LaunchpadSoyuz extends BlockDummyable {

    public LaunchpadSoyuz(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new LaunchpadSoyuzBlockEntity(LaunchInfraBlockEntities.LAUNCHPAD_SOYUZ.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == LaunchInfraBlockEntities.LAUNCHPAD_SOYUZ.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this.standardOpenBehavior(level, pos, player);
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 2, 2, 2, 2};
    }

    @Override
    public int[][] getAllDimensions() {
        return new int[][]{
                {2, 0, 2, 2, 2, 2},
                {3, 0, 2, 1, 1, 2},

                {2, -2, 2, 2, -2, 10},
                {2, -2, 10, -2, 2, 10},
                {3, -3, 9, 1, 1, 9},
                {1, 0, 2, 2, -6, 10},
                {1, 0, 10, -6, 2, 2},
                {1, 0, 10, -6, -6, 10},

                {0, 0, 0, 0, -10, 58, 2, 0, 0},
                {1, -1, 0, 0, -56, 58, 2, 0, 0},
                {0, 0, 0, 0, -10, 58, -10, 0, 0},
                {1, -1, 0, 0, -56, 58, -10, 0, 0},

                {2, 0, 2, 1, 7, -3},
                {2, 0, 2, 1, 7, -3, -6, 0, 0},
                {0, 0, 1, 1, 7, -3, -4, 2, 0},
                {1, -1, 5, 5, 7, -3, -4, 2, 0},
                {3, -2, 4, 4, 7, -3, -4, 2, 0},
                {6, -4, 3, 3, 7, -3, -4, 2, 0},
                {51, -7, 2, 2, 7, -3, -4, 2, 0},

                {7, 0, -6, 7, 7, -3},
        };
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        if (!super.checkRequirement(level, placedPos, dir, placementOffset)) return false;

        BlockPos core = placedPos.relative(dir, placementOffset);
        int dx = dir.getStepX();
        int dz = dir.getStepZ();

        return checkAll(level, core, dx, dz, placedPos, dir);
    }

    private boolean checkAll(Level level, BlockPos core, int dx, int dz, BlockPos placedPos, Direction dir) {
        // Mirrors fillSpace's exact pivot list (see that method) - CE checks the same 19 shapes it fills.
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{3, 0, 2, 1, 1, 2}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(0, 2, 0), new int[]{0, 0, 2, 2, -2, 10}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(0, 2, 0), new int[]{0, 0, 10, -2, 2, 10}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -3, 9, 1, 1, 9}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(0, 2, 0), new int[]{-1, 2, 2, 2, -6, 10}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(0, 2, 0), new int[]{-1, 2, 10, -6, 2, 2}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(0, 2, 0), new int[]{-1, 2, 10, -6, -6, 10}, placedPos, dir)) return false;

        if (!MultiblockHandlerXR.checkSpace(level, core.offset(2 * dx, 0, 2 * dz), new int[]{0, 0, 0, 0, -10, 58}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(2 * dx, 0, 2 * dz), new int[]{1, -1, 0, 0, -56, 58}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-10 * dx, 0, -10 * dz), new int[]{0, 0, 0, 0, -10, 58}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-10 * dx, 0, -10 * dz), new int[]{1, -1, 0, 0, -56, 58}, placedPos, dir)) return false;

        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{2, 0, 2, 1, 7, -3}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-7 * dx, 0, -7 * dz), new int[]{2, 0, 2, 1, 7, -3}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{0, 0, 1, 1, 7, -3}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{1, -1, 5, 5, 7, -3}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{3, -2, 4, 4, 7, -3}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{6, -4, 3, 3, 7, -3}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{51, -7, 2, 2, 7, -3}, placedPos, dir)) return false;

        return MultiblockHandlerXR.checkSpace(level, core.offset(-4 * dx, 0, -4 * dz), new int[]{7, 0, -6, 7, 7, -3}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);

        BlockPos core = placedPos.relative(dir, placementOffset);
        int dx = dir.getStepX();
        int dz = dir.getStepZ();

        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, 0, 2, 1, 1, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(0, 2, 0), new int[]{0, 0, 2, 2, -2, 10}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(0, 2, 0), new int[]{0, 0, 10, -2, 2, 10}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -3, 9, 1, 1, 9}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(0, 2, 0), new int[]{-1, 2, 2, 2, -6, 10}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(0, 2, 0), new int[]{-1, 2, 10, -6, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(0, 2, 0), new int[]{-1, 2, 10, -6, -6, 10}, this, dir);

        MultiblockHandlerXR.fillSpace(level, core.offset(2 * dx, 0, 2 * dz), new int[]{0, 0, 0, 0, -10, 58}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(2 * dx, 0, 2 * dz), new int[]{1, -1, 0, 0, -56, 58}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-10 * dx, 0, -10 * dz), new int[]{0, 0, 0, 0, -10, 58}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-10 * dx, 0, -10 * dz), new int[]{1, -1, 0, 0, -56, 58}, this, dir);

        MultiblockHandlerXR.fillSpace(level, core, new int[]{2, 0, 2, 1, 7, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-7 * dx, 0, -7 * dz), new int[]{2, 0, 2, 1, 7, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{0, 0, 1, 1, 7, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{1, -1, 5, 5, 7, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{3, -2, 4, 4, 7, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{6, -4, 3, 3, 7, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.offset(-4 * dx, 2, -4 * dz), new int[]{51, -7, 2, 2, 7, -3}, this, dir);

        MultiblockHandlerXR.fillSpace(level, core.offset(-4 * dx, 0, -4 * dz), new int[]{7, 0, -6, 7, 7, -3}, this, dir);
    }
}
