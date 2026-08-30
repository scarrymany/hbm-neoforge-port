package com.hbm.blocks.network;

import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.FluidValveBlockEntity;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Redstone-driven twin of {@link FluidValveBlock}, ported from CE's
 * {@code com.hbm.blocks.network.FluidSwitch} - shares {@link FluidValveBlock#ACTIVE} and
 * {@link FluidValveBlockEntity} exactly like CE's own {@code FluidSwitch} shares
 * {@code TileEntityFluidValve} with {@code FluidValve}. Mirrors the sibling energy family's
 * {@code CableDetectorBlock}/{@code CableSwitchBlock} split (same wave, same pattern).
 */
public class FluidSwitchBlock extends FluidDuctBaseBlock {

    public static final BooleanProperty ACTIVE = FluidValveBlock.ACTIVE;

    public FluidSwitchBlock(Properties properties) {
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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide) return;

        boolean on = level.hasNeighborSignal(pos);
        boolean wasOn = state.getValue(ACTIVE);
        if (on == wasOn) return;

        level.setBlock(pos, state.setValue(ACTIVE, on), 2);
        level.playSound(null, pos, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 1.0F, on ? 1.0F : 0.85F);
        if (level.getBlockEntity(pos) instanceof FluidValveBlockEntity valve) valve.updateState();
    }
}
