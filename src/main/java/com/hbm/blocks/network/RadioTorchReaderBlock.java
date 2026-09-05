package com.hbm.blocks.network;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.network.RadioNetworkBlockEntities;
import com.hbm.blockentity.network.RadioTorchReaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** CE {@code RadioTorchReader} — stay only on {@link IRORValueProvider}. */
public class RadioTorchReaderBlock extends RadioTorchBaseBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchReaderBlockEntity(RadioNetworkBlockEntities.READER.get(), pos, state);
    }

    @Override
    public boolean canBlockStay(LevelReader level, Direction dir, BlockPos support, BlockState supportState) {
        BlockEntity te = level.getBlockEntity(support);
        return te instanceof IRORValueProvider;
    }
}
