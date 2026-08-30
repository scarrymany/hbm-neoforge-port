package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blockentity.machine.SolarBoilerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code MachineSolarBoiler} (regname {@code machine_solar_boiler}). No GUI, no
 * inventory - a pure fluid producer fed externally by a {@link com.hbm.blocks.machine.SolarMirrorBlock}.
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
