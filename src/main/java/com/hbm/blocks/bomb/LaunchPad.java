package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.LaunchPadBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.LaunchPad} (98 lines, read in full) - the small,
 * single-missile pad. {@code BlockDummyable} subclass, {@code getDimensions() = {0,0,1,1,1,1}}
 * (single dummy ring, offset 1), {@code implements IBomb} to route detonator-triggered launches
 * through {@link LaunchPadBlockEntity#launchFromDesignator()}.
 */
public class LaunchPad extends BlockDummyable implements IBomb {

    public LaunchPad(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, -0.5D, 1D, -0.5D));
        this.bounding.add(new AABB(0.5D, 0D, -1.5D, 1.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-1.5D, 0D, 0.5D, -0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(0.5D, 0D, 0.5D, 1.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-0.5D, 0.5D, -1.5D, 0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-1.5D, 0.5D, -0.5D, 1.5D, 1D, 0.5D));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new LaunchPadBlockEntity(BombBlockEntities.LAUNCH_PAD.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BombBlockEntities.LAUNCH_PAD.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this.standardOpenBehavior(level, pos, player);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof LaunchPadBlockEntity entity) {
                return entity.launchFromDesignator();
            }
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof LaunchPadBlockEntity launchPad) {
                launchPad.updateRedstonePower(pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);

        BlockPos core = placedPos.relative(dir, placementOffset);
        this.makeExtra(level, core.offset(1, 0, 1));
        this.makeExtra(level, core.offset(1, 0, -1));
        this.makeExtra(level, core.offset(-1, 0, 1));
        this.makeExtra(level, core.offset(-1, 0, -1));
    }
}
