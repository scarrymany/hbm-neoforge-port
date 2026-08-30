package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.PipeBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plain connector duct, ported from CE's {@code com.hbm.blocks.network.FluidDuctStandard} (rendering
 * concerns - {@code IDynamicModels}, per-connection-mask collision boxes, the {@code META}
 * silver/colored texture variants - all deferred to Phase 5, see {@link FluidDuctBaseBlock}'s javadoc).
 */
public class FluidDuctStandardBlock extends FluidDuctBaseBlock {

    public FluidDuctStandardBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBaseBlockEntity(FluidDuctBlockEntities.STANDARD_TYPE.get(), pos, state);
    }
}
