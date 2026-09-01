package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineCompressorBlockEntity;
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

/** CE {@code MachineCompressor} — Dummyable {2,0,1,2,1,1} offset 2 + extras. */
public class MachineCompressorBlock extends BlockDummyable {

    public MachineCompressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineCompressorBlockEntity(DummyableProcessBlockEntities.MACHINE_COMPRESSOR.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_COMPRESSOR.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return super.checkRequirement(level, placedPos, dir, placementOffset)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -3, 1, 1, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{8, -4, 0, 0, 1, 1}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -3, 1, 1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{8, -4, 0, 0, 1, 1}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(dir.getOpposite()));
        makeExtra(level, core.relative(rot));
        makeExtra(level, core.relative(rot.getOpposite()));
    }
}
