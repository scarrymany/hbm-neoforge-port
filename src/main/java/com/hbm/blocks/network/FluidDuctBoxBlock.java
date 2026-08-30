package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.PipeBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Larger-footprint (box) duct variant, ported from CE's {@code com.hbm.blocks.network.FluidDuctBox} -
 * shares {@link PipeBaseBlockEntity} with {@link FluidDuctStandardBlock} (same {@code TileEntityPipeBaseNT}
 * pairing in CE). The wall-thickness {@code META} cosmetic tiers and junction/curve baked-model
 * assembly are deferred to Phase 5, see {@link FluidDuctBaseBlock}'s javadoc.
 */
public class FluidDuctBoxBlock extends FluidDuctBaseBlock {

    public FluidDuctBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBaseBlockEntity(FluidDuctBlockEntities.BOX_TYPE.get(), pos, state);
    }
}
