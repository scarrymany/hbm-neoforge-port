package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineCokerBlockEntity;
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

/** CE {@code MachineCoker} — Dummyable {22,0,1,1,1,1} offset 1 + stack extras. */
public class MachineCokerBlock extends BlockDummyable {

    public MachineCokerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{22, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineCokerBlockEntity(DummyableProcessBlockEntities.MACHINE_COKER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_COKER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        if (!super.checkRequirement(level, placedPos, dir, placementOffset)) return false;
        BlockPos core = placedPos.relative(dir, placementOffset);
        return MultiblockHandlerXR.checkSpace(level, core.above(), new int[]{5, 0, 2, 2, 2, 2}, placedPos, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(level, core.offset(2, 1, 2), new int[]{0, 1, 0, 0, 0, 0}, placedPos, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(level, core.offset(2, 1, -2), new int[]{0, 1, 0, 0, 0, 0}, placedPos, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(level, core.offset(-2, 1, 2), new int[]{0, 1, 0, 0, 0, 0}, placedPos, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(level, core.offset(-2, 1, -2), new int[]{0, 1, 0, 0, 0, 0}, placedPos, Direction.NORTH);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core.above(), new int[]{5, 0, 2, 2, 2, 2}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, core.offset(2, 1, 2), new int[]{0, 1, 0, 0, 0, 0}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, core.offset(2, 1, -2), new int[]{0, 1, 0, 0, 0, 0}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, core.offset(-2, 1, 2), new int[]{0, 1, 0, 0, 0, 0}, this, Direction.NORTH);
        MultiblockHandlerXR.fillSpace(level, core.offset(-2, 1, -2), new int[]{0, 1, 0, 0, 0, 0}, this, Direction.NORTH);
        makeExtra(level, core.offset(1, 0, 1));
        makeExtra(level, core.offset(1, 0, -1));
        makeExtra(level, core.offset(-1, 0, 1));
        makeExtra(level, core.offset(-1, 0, -1));
    }
}
