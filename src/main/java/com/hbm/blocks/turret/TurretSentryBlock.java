package com.hbm.blocks.turret;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.blockentity.turret.TurretSentryBlockEntity;
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

/**
 * Ported from CE's {@code TurretSentry} - unlike every other concrete turret this one is a plain
 * single {@code BlockContainer} (CE: {@code NTMBlockContainer}), not a
 * {@link com.hbm.blocks.BlockDummyable} multiblock casing at all (confirmed by reading the CE
 * source - see {@code docs/phase3/turret_system.md}'s substrate table). CE's scattered-loot
 * {@code breakBlock} override (dropping the ammo inventory in small randomized batches rather than
 * the standard single stack) is not reproduced - vanilla's own
 * {@code Containers.dropContentsOnDestroy}-driven default (used by every other machine in this
 * port) already drops the full inventory correctly, just as one stack per slot rather than several
 * partial stacks; a cosmetic-only difference, not a functional one.
 */
public class TurretSentryBlock extends BaseEntityBlock {

    public TurretSentryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretSentryBlockEntity(TurretBlockEntities.SENTRY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TurretBlockEntities.SENTRY.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TurretSentryBlockEntity sentry) {
            player.openMenu(sentry, pos);
        }
        return InteractionResult.CONSUME;
    }
}
