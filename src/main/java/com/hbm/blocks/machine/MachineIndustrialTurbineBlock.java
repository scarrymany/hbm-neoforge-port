package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineIndustrialTurbineBlockEntity;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code MachineIndustrialTurbine} (regname {@code machine_industrial_turbine}).
 * No GUI, no inventory in CE either - confirmed by source (implements neither {@code IGUIProvider}
 * nor holds an {@code ItemStackHandler}), see {@link MachineIndustrialTurbineBlockEntity}'s javadoc.
 */
public class MachineIndustrialTurbineBlock extends BlockDummyable {

    public MachineIndustrialTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 3, 1, 1};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineIndustrialTurbineBlockEntity(PowerGenBlockEntities.INDUSTRIAL_TURBINE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.INDUSTRIAL_TURBINE.get() ? ITickableBE.ticker() : null;
    }
}
