package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineRadarBlockEntity;
import com.hbm.blockentity.machine.SensorBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** CE {@code MachineRadarNT} / {@code MachineRadarLarge} — single-block scanner + comparator. */
public class MachineRadarBlock extends BaseEntityBlock {

    public static final MapCodec<MachineRadarBlock> CODEC = simpleCodec(p -> new MachineRadarBlock(p, false));

    private final boolean large;

    public MachineRadarBlock(Properties properties) {
        this(properties, false);
    }

    public MachineRadarBlock(Properties properties, boolean large) {
        super(properties);
        this.large = large;
    }

    public boolean isLarge() {
        return large;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineRadarBlockEntity(
                large ? SensorBlockEntities.MACHINE_RADAR_LARGE.get() : SensorBlockEntities.MACHINE_RADAR.get(),
                pos, state, large);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityType<?> expected = large ? SensorBlockEntities.MACHINE_RADAR_LARGE.get() : SensorBlockEntities.MACHINE_RADAR.get();
        return type == expected ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineRadarBlockEntity radar) {
            player.openMenu(radar, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineRadarBlockEntity radar) {
            return radar.getRedPower();
        }
        return 0;
    }
}
