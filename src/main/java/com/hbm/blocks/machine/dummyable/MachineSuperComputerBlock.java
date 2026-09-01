package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineSuperComputerBlockEntity;
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
 * CE {@code MachineSuperComputer} — Dummyable {5,0,3,3,3,3} offset 8 + XR extras.
 * Replaces the Phase11 casing of the same id.
 */
public class MachineSuperComputerBlock extends BlockDummyable {

    public MachineSuperComputerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 3, 3, 3, 3};
    }

    @Override
    public int getOffset() {
        return 8;
    }

    @Override
    public int[][] getAllDimensions() {
        return new int[][]{
                getDimensions(),
                new int[]{6, -6, 3, 3, 1, 1},
                new int[]{6, -6, 1, 1, 3, 3},
                new int[]{7, -7, 1, 1, 1, 1},
                new int[]{2, 0, -3, 8, 1, 1}
        };
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        for (int[] dim : getAllDimensions()) {
            if (!MultiblockHandlerXR.checkSpace(level, core, dim, placedPos, dir)) return false;
        }
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineSuperComputerBlockEntity(DummyableProcessBlockEntities.MACHINE_SUPERCOMPUTER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_SUPERCOMPUTER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{6, -6, 3, 3, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{6, -6, 1, 1, 3, 3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{7, -7, 1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{2, 0, -3, 8, 1, 1}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(dir, 8));
        makeExtra(level, core.relative(dir, 7).relative(rot));
        makeExtra(level, core.relative(dir, 7).relative(rot.getOpposite()));
        makeExtra(level, core.relative(dir, 5).relative(rot));
        makeExtra(level, core.relative(dir, 5).relative(rot.getOpposite()));
    }
}
