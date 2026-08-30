package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidCounterValveBlockEntity;
import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click-toggled fluid-throughput counter, ported from CE's
 * {@code com.hbm.blocks.network.FluidCounterValve} ({@code META} 0/1 -&gt; {@link #ACTIVE}).
 */
public class FluidCounterValveBlock extends FluidDuctBaseBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public FluidCounterValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidCounterValveBlockEntity(FluidDuctBlockEntities.COUNTER_VALVE_TYPE.get(), pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        boolean next = !state.getValue(ACTIVE);
        level.setBlock(pos, state.setValue(ACTIVE, next), 2);
        level.playSound(null, pos, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 1.0F, next ? 1.0F : 0.85F);

        if (level.getBlockEntity(pos) instanceof FluidCounterValveBlockEntity valve) valve.updateState();
        return InteractionResult.CONSUME;
    }
}
