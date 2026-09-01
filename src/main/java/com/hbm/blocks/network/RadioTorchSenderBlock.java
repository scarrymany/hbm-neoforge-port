package com.hbm.blocks.network;

import com.hbm.blockentity.network.RadioNetworkBlockEntities;
import com.hbm.blockentity.network.RadioTorchSenderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** CE {@code RadioTorchSender}. */
public class RadioTorchSenderBlock extends RadioTorchBaseBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchSenderBlockEntity(RadioNetworkBlockEntities.SENDER.get(), pos, state);
    }
}
