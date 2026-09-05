package com.hbm.blocks.network;

import com.hbm.blockentity.network.RadioNetworkBlockEntities;
import com.hbm.blockentity.network.RadioTorchReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** CE {@code RadioTorchReceiver} — weak power = lastState. */
public class RadioTorchReceiverBlock extends RadioTorchBaseBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        RadioTorchReceiverBlockEntity tile = new RadioTorchReceiverBlockEntity(
                RadioNetworkBlockEntities.RECEIVER.get(), pos, state);
        return tile;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockEntity(pos) instanceof RadioTorchReceiverBlockEntity rec) {
            return rec.lastState;
        }
        return 0;
    }
}
