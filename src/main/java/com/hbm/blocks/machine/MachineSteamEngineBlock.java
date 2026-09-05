package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineSteamEngineBlockEntity;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code MachineSteamEngine} (regname {@code machine_steam_engine}): a
 * {@link BlockDummyable} multiblock, {@code {1,0,5,1,1,1}} dimensions, offset 1. No GUI, no
 * inventory - only the core block position (meta 12-15) carries a block entity; every dummy
 * position (meta 0-11) has none, matching CE's plain-dummy range (CE's "extra"-flagged dummies carry
 * a capability-forwarding proxy tile entity, {@code TileEntityProxyCombo}, which this port does not
 * have - see the research report's simplification note on this class's block-entity javadoc: the
 * core's own fixed connector-position math already reaches the right neighbor blocks directly,
 * without needing the dummy itself to forward anything). fillSpace extras Exact CE {@code :49-59}.
 */
public class MachineSteamEngineBlock extends BlockDummyable {

    public MachineSteamEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 5, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * Exact CE {@code MachineSteamEngine.fillSpace} extras ({@code MachineSteamEngine.java:49-59}).
     * After {@code super.fillSpace}: add {@code dir * o} (core), {@code rot = dir} clockwise around Y,
     * then three extras at {@code y+1} along the rot face — {@code core+rot}, {@code core+rot+dir},
     * {@code core+rot-dir}. No ProxyCombo TE — extras are {@code makeExtra} flags only.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(rot).above());
        makeExtra(level, core.relative(rot).relative(dir).above());
        makeExtra(level, core.relative(rot).relative(dir.getOpposite()).above());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineSteamEngineBlockEntity(PowerGenBlockEntities.STEAM_ENGINE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.STEAM_ENGINE.get() ? ITickableBE.ticker() : null;
    }
}
