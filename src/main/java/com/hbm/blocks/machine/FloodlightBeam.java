package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.FloodlightBeamBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloodlightBeam extends BlockBeamBase {

    public static final MapCodec<FloodlightBeam> CODEC = simpleCodec(p -> new FloodlightBeam());

    @Override
    protected @NotNull MapCodec<? extends FloodlightBeam> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FloodlightBeamBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FloodlightBeamBlockEntity beam) {
                beam.tick();
            }
        };
    }
}
