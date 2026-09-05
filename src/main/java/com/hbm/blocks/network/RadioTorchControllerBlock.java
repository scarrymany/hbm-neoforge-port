package com.hbm.blocks.network;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.network.RadioNetworkBlockEntities;
import com.hbm.blockentity.network.RadioTorchControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** CE {@code RadioTorchController} — stay on {@link IRORValueProvider} (CE block check). */
public class RadioTorchControllerBlock extends RadioTorchBaseBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchControllerBlockEntity(RadioNetworkBlockEntities.CONTROLLER.get(), pos, state);
    }

    @Override
    public boolean canBlockStay(LevelReader level, Direction dir, BlockPos support, BlockState supportState) {
        BlockEntity te = level.getBlockEntity(support);
        return te instanceof IRORValueProvider;
    }
}
