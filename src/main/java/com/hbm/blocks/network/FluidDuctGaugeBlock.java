package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.PipeGaugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Read-only fill-level/throughput display duct, ported from CE's
 * {@code com.hbm.blocks.network.FluidDuctGauge}. The placement-facing {@code FACING} property is
 * cosmetic only in CE (never read by {@code TileEntityPipeGauge}'s own logic) and is dropped here per
 * this package's "keep gameplay-gating state, drop pure model-selection state" scope reduction - see
 * {@link FluidDuctBaseBlock}'s javadoc; a future Phase 5 pass can reintroduce it once a real baked
 * model reads it.
 */
public class FluidDuctGaugeBlock extends FluidDuctBaseBlock {

    public FluidDuctGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeGaugeBlockEntity(FluidDuctBlockEntities.GAUGE_TYPE.get(), pos, state);
    }
}
