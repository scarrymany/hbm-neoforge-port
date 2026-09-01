package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** CE {@code MachineTowerSmall} — Dummyable {18,0,2,2,2,2} offset 2. Fluid condenser, not a chimney. */
public class MachineTowerSmallBlock extends BlockDummyable {

    public MachineTowerSmallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{18, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? CondenserBlockEntity.towerSmall(DummyableProcessBlockEntities.MACHINE_TOWER_SMALL.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_TOWER_SMALL.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
