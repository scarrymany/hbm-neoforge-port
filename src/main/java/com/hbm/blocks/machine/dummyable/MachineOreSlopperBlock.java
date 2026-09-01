package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineOreSlopperBlockEntity;
import com.hbm.blocks.BlockDummyable;
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

/** CE {@code MachineOreSlopper} — Dummyable {3,0,3,3,1,1} offset 3 + 8 extras. */
public class MachineOreSlopperBlock extends BlockDummyable {

    public MachineOreSlopperBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 3, 3, 1, 1};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineOreSlopperBlockEntity(DummyableProcessBlockEntities.MACHINE_ORE_SLOPPER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_ORE_SLOPPER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(dir, 3));
        makeExtra(level, core.relative(dir, -3));
        makeExtra(level, core.relative(rot));
        makeExtra(level, core.relative(rot.getOpposite()));
        makeExtra(level, core.relative(dir, 2).relative(rot));
        makeExtra(level, core.relative(dir, 2).relative(rot.getOpposite()));
        makeExtra(level, core.relative(dir, -2).relative(rot));
        makeExtra(level, core.relative(dir, -2).relative(rot.getOpposite()));
    }
}
