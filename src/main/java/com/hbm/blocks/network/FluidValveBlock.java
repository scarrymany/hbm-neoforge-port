package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.FluidValveBlockEntity;
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
 * Right-click-toggled on/off duct valve, ported from CE's {@code com.hbm.blocks.network.FluidValve}
 * ({@code META} 0/1 -&gt; {@link #ACTIVE}). {@link #ACTIVE} is shared with {@link FluidSwitchBlock}
 * (both pair with {@code FluidValveBlockEntity} in CE too, see
 * {@code docs/phase2/network_fluid_ducts.md}'s registry table).
 */
public class FluidValveBlock extends FluidDuctBaseBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public FluidValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidValveBlockEntity(FluidDuctBlockEntities.VALVE_TYPE.get(), pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        boolean next = !state.getValue(ACTIVE);
        level.setBlock(pos, state.setValue(ACTIVE, next), 2);
        level.playSound(null, pos, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 1.0F, next ? 1.0F : 0.85F);

        if (level.getBlockEntity(pos) instanceof FluidValveBlockEntity valve) valve.updateState();
        return InteractionResult.CONSUME;
    }
}
