package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blockentity.machine.SolarBoilerBlockEntity;
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
 * Ported from CE's {@code MachineSolarBoiler} (regname {@code machine_solar_boiler}). No GUI, no
 * inventory - a pure fluid producer fed externally by a {@link com.hbm.blocks.machine.SolarMirrorBlock}.
 * fillSpace extras Exact CE {@code :50-56}.
 */
public class MachineSolarBoilerBlock extends BlockDummyable {

    public MachineSolarBoilerBlock(Properties properties) {
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
     * Exact CE {@code MachineSolarBoiler.fillSpace} extras ({@code MachineSolarBoiler.java:50-56}).
     * After {@code super.fillSpace} (AABB only): one top extra at {@code core.y+2}. CE first adds
     * {@code dir * o} then {@code makeExtra(x, y+2, z)}. No ProxyCombo TE — extras are {@code makeExtra}
     * flags only (same as HeatBoiler / industrial boiler).
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        makeExtra(level, placedPos.relative(dir, placementOffset).above(2));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new SolarBoilerBlockEntity(PowerGenBlockEntities.SOLAR_BOILER.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.SOLAR_BOILER.get() ? ITickableBE.ticker() : null;
    }
}
