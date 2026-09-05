package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.ReactorZirnoxBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code ReactorZirnox} — Dummyable {1,0,2,2,2,2} offset 2 + 3 XR extras.
 * TODO(CE: ReactorZirnox.java:33): TileEntityProxyCombo(true,true,true) on extras.
 * TODO(CE: ReactorZirnox.java:43): BossSpawnHandler.markFBI.
 * TODO(CE: StaticTesrBakedModels.java:315): OBJ TESR yaw map.
 */
public class ReactorZirnoxBlock extends BlockDummyable {

    public ReactorZirnoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new ReactorZirnoxBlockEntity(DummyableProcessBlockEntities.REACTOR_ZIRNOX.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.REACTOR_ZIRNOX.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return MultiblockHandlerXR.checkSpace(level, core, getDimensions(), placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, -2, 1, 1, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, -2, 0, 0, 2, -2}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, -2, 0, 0, -2, 2}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, -2, 1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, -2, 0, 0, 2, -2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, -2, 0, 0, -2, 2}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.offset(rot.getStepX() * 2, 1, rot.getStepZ() * 2));
        makeExtra(level, core.offset(rot.getStepX() * 2, 3, rot.getStepZ() * 2));
        makeExtra(level, core.offset(-rot.getStepX() * 2, 1, -rot.getStepZ() * 2));
        makeExtra(level, core.offset(-rot.getStepX() * 2, 3, -rot.getStepZ() * 2));
        makeExtra(level, core.above(4));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;
        BlockPos core = findCore(level, pos);
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof ReactorZirnoxBlockEntity te)) return;
        boolean powered = false;
        for (int dx = -2; dx <= 2 && !powered; dx++) {
            for (int dy = 0; dy <= 4 && !powered; dy++) {
                for (int dz = -2; dz <= 2 && !powered; dz++) {
                    if (dx == -2 || dx == 2 || dy == 0 || dy == 4 || dz == -2 || dz == 2) {
                        if (level.hasNeighborSignal(core.offset(dx, dy, dz))) {
                            powered = true;
                        }
                    }
                }
            }
        }
        te.setRedstonePowered(powered);
    }
}
