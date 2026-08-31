package com.hbm.blocks.turret;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.blockentity.turret.TurretSentryDamagedBlockEntity;
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
import com.mojang.serialization.MapCodec;

/**
 * Ported from CE's {@code TurretSentryDamaged} - a ruins/loot "damaged" variant, single block
 * (CE: plain {@code BlockContainer}), drops nothing when broken (CE's own
 * {@code getItemDropped -> Items.AIR}, reproduced via {@code Properties.noLootTable()}/no
 * {@code BlockItem}, applied where this block is registered).
 */
public class TurretSentryDamagedBlock extends BaseEntityBlock {

    public static final MapCodec<TurretSentryDamagedBlock> CODEC = simpleCodec(TurretSentryDamagedBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public TurretSentryDamagedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretSentryDamagedBlockEntity(TurretBlockEntities.SENTRY_DAMAGED.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TurretBlockEntities.SENTRY_DAMAGED.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TurretSentryDamagedBlockEntity sentry) {
            player.openMenu(sentry, pos);
        }
        return InteractionResult.CONSUME;
    }
}
