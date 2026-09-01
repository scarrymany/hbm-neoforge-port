package com.hbm.blocks.network;

import com.hbm.blockentity.network.RadioNetworkBlockEntities;
import com.hbm.blockentity.network.RadioTorchCounterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** CE {@code RadioTorchCounter}. */
public class RadioTorchCounterBlock extends RadioTorchBaseBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchCounterBlockEntity(RadioNetworkBlockEntities.COUNTER.get(), pos, state);
    }
}
