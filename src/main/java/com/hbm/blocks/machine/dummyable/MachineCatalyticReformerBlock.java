package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineCatalyticReformerBlockEntity;
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

/** CE {@code MachineCatalyticReformer} — Dummyable {2,0,1,1,2,2} offset 1 + extras. */
public class MachineCatalyticReformerBlock extends BlockDummyable {

    public MachineCatalyticReformerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineCatalyticReformerBlockEntity(DummyableProcessBlockEntities.MACHINE_CATALYTIC_REFORMER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CATALYTIC_REFORMER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return super.checkRequirement(level, placedPos, dir, placementOffset)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -3, 1, 0, -1, 2}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{6, -3, 1, 1, 2, 0}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -3, 1, 0, -1, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{6, -3, 1, 1, 2, 0}, this, dir);
        Direction rot = dir.getClockWise();
        BlockPos back = core.relative(dir.getOpposite());
        makeExtra(level, back.offset(1, 0, 1));
        makeExtra(level, back.offset(1, 0, -1));
        makeExtra(level, back.offset(-1, 0, 1));
        makeExtra(level, back.offset(-1, 0, -1));
        makeExtra(level, back.relative(rot, 2));
        makeExtra(level, back.relative(rot.getOpposite(), 2));
    }
}
