package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.CableSwitchBlockEntity;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.CableSwitch}: a right-click toggle that
 * gates {@link CableSwitchBlockEntity#shouldCreateNode()} on {@link #STATE} (CE's meta 0/1 off/on).
 */
public class CableSwitchBlock extends BaseEntityBlock {

    public static final BooleanProperty STATE = BooleanProperty.create("state");

    public CableSwitchBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        boolean isOn = state.getValue(STATE);
        level.setBlock(pos, state.setValue(STATE, !isOn), 2);
        level.playSound(null, pos, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 1.0F, isOn ? 0.85F : 1.0F);

        if (level.getBlockEntity(pos) instanceof CableSwitchBlockEntity te) te.updateState();
        return InteractionResult.CONSUME;
    }
}
