package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.PipeExhaustBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exhaust/vent box-duct variant, ported from CE's {@code com.hbm.blocks.network.FluidDuctBoxExhaust
 * extends FluidDuctBox} (the one second-level subclass in this family). Pairs with
 * {@link PipeExhaustBlockEntity} instead of {@link com.hbm.blockentity.network.PipeBaseBlockEntity} -
 * matches CE's own {@code createNewTileEntity} override returning {@code TileEntityPipeExhaust}.
 */
public class FluidDuctBoxExhaustBlock extends FluidDuctBoxBlock {

    public FluidDuctBoxExhaustBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeExhaustBlockEntity(FluidDuctBlockEntities.BOX_EXHAUST_TYPE.get(), pos, state);
    }
}
