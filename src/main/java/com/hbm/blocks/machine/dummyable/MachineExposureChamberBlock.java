package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineExposureChamberBlockEntity;
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

/** CE {@code MachineExposureChamber} — Dummyable {4,0,2,2,2,2} offset 2 + XR beam + 5 extras. */
public class MachineExposureChamberBlock extends BlockDummyable {

    public MachineExposureChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineExposureChamberBlockEntity(DummyableProcessBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_EXPOSURE_CHAMBER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getCounterClockWise();
        if (!MultiblockHandlerXR.checkSpace(level, core, getDimensions(), placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{3, 0, 0, 0, -3, 8}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{0, 0, 1, -1, -3, 6}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{0, 0, -1, 1, -3, 6}, placedPos, dir)) return false;
        BlockPos far = core.relative(rot, 7);
        if (!MultiblockHandlerXR.checkSpace(level, far, new int[]{3, 0, 1, -1, 0, 1}, placedPos, dir)) return false;
        return MultiblockHandlerXR.checkSpace(level, far, new int[]{3, 0, -1, 1, 0, 1}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getCounterClockWise();
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, 0, 0, 0, -3, 8}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.above(2), new int[]{0, 0, 1, -1, -3, 6}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core.above(2), new int[]{0, 0, -1, 1, -3, 6}, this, dir);
        BlockPos far = core.relative(rot, 7);
        MultiblockHandlerXR.fillSpace(level, far, new int[]{3, 0, 1, -1, 0, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, far, new int[]{3, 0, -1, 1, 0, 1}, this, dir);
        makeExtra(level, far.relative(dir));
        makeExtra(level, far.relative(dir.getOpposite()));
        BlockPos tip = core.relative(rot, 8);
        makeExtra(level, tip.relative(dir));
        makeExtra(level, tip.relative(dir.getOpposite()));
        makeExtra(level, tip);
    }
}
