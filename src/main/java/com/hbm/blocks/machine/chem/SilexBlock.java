package com.hbm.blocks.machine.chem;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.chem.ChemIsotopeBlockEntities;
import com.hbm.blockentity.machine.chem.SilexBlockEntity;
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

/** Ported from CE's {@code MachineSILEX} (regname {@code machine_silex}) - laser isotope/element separation. fillSpace extras Exact CE {@code :62-74}. */
public class SilexBlock extends BlockDummyable {

    public SilexBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * Exact CE {@code MachineSILEX.fillSpace} extras ({@code MachineSILEX.java:62-74}).
     * After {@code super.fillSpace}: extras at {@code core.y+1} perpendicular to facing —
     * NS → {@code ±X}, EW → {@code ±Z}. No ProxyCombo TE — extras are {@code makeExtra} flags only.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        if (dir == Direction.NORTH || dir == Direction.SOUTH) {
            makeExtra(level, core.offset(1, 1, 0));
            makeExtra(level, core.offset(-1, 1, 0));
        }
        if (dir == Direction.EAST || dir == Direction.WEST) {
            makeExtra(level, core.offset(0, 1, 1));
            makeExtra(level, core.offset(0, 1, -1));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new SilexBlockEntity(ChemIsotopeBlockEntities.SILEX.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ChemIsotopeBlockEntities.SILEX.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
