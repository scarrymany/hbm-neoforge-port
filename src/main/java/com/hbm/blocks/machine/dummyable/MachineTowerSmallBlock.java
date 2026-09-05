package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
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

/** CE {@code MachineTowerSmall} — Dummyable {18,0,2,2,2,2} offset 2. Fluid condenser, not a chimney. fillSpace extras Exact CE {@code :48-58}. */
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

    /**
     * Exact CE {@code MachineTowerSmall.fillSpace} extras ({@code MachineTowerSmall.java:48-58}).
     * After {@code super.fillSpace}: add {@code dir * o} (core), then {@code makeExtra} at
     * {@code core + dr2 * 2} for CE {@code ForgeDirection} ids {@code 2..6} (N/S/W/E + {@code UNKNOWN}
     * which is {@code (0,0,0)} = the core itself). No ProxyCombo TE — extras are {@code makeExtra}
     * flags only.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        makeExtra(level, core.relative(Direction.NORTH, 2));
        makeExtra(level, core.relative(Direction.SOUTH, 2));
        makeExtra(level, core.relative(Direction.WEST, 2));
        makeExtra(level, core.relative(Direction.EAST, 2));
        makeExtra(level, core);
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
