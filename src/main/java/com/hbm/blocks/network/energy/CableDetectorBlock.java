package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.CableSwitchBlockEntity;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.CableDetector} - a redstone-driven twin of
 * {@link CableSwitchBlock} sharing the exact same {@link CableSwitchBlockEntity} (CE's own comment:
 * "same TE"). {@link #STATE} tracks whether the block is currently receiving a redstone signal
 * ({@link Level#hasNeighborSignal}, the 1.21 equivalent of CE's {@code World.isBlockPowered}), driven
 * from {@link #neighborChanged} rather than a player right-click.
 */
public class CableDetectorBlock extends BaseEntityBlock {

    public static final BooleanProperty STATE = CableSwitchBlock.STATE;

    public CableDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STATE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableSwitchBlockEntity(EnergyNetworkBlockEntities.CABLE_SWITCH.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.CABLE_SWITCH.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide) return;

        boolean on = level.hasNeighborSignal(pos);
        boolean wasOn = state.getValue(STATE);
        if (on == wasOn) return;

        level.setBlock(pos, state.setValue(STATE, on), 2);
        level.playSound(null, pos, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 1.0F, on ? 1.0F : 0.85F);
        if (level.getBlockEntity(pos) instanceof CableSwitchBlockEntity te) te.updateState();
    }
}
