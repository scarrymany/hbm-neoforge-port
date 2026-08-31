package com.hbm.blocks.machine.reprocess;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.reprocess.ReprocessBlockEntities;
import com.hbm.blockentity.machine.reprocess.SolidifierBlockEntity;
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

/** CE {@code MachineSolidifier} / {@code machine_solidifier}. Fluid → item. */
public class SolidifierBlock extends BaseEntityBlock {

    public static final MapCodec<SolidifierBlock> CODEC = simpleCodec(SolidifierBlock::new);

    public SolidifierBlock(Properties properties) {
        super(properties);
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
        return new SolidifierBlockEntity(ReprocessBlockEntities.MACHINE_SOLIDIFIER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ReprocessBlockEntities.MACHINE_SOLIDIFIER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof SolidifierBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }
}
