package com.hbm.blocks.bomb;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.bomb.NukeBalefireBlockEntity;
import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.ITickableBE;
import com.hbm.main.ModContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code NukeBalefire} (128 lines, read in full) - unlike the other 8 casings this
 * one is not a flat "ready check then spawn" shape: {@code explode()} lives on the ticking block
 * entity itself (arm/countdown), and the block's own {@link #explode} just smuggles the triggering
 * {@link Entity} through {@link ModContext#DETONATOR_CONTEXT} around that call, exactly like CE's
 * {@code try { ModContext.DETONATOR_CONTEXT.set(detonator); bomb.explode(); } finally { ...remove(); }}
 * - see {@code docs/phase3/bomb_blocks_and_detonators.md}'s "Key design/API decisions" note on this
 * thread-local's discipline. Facing/GUI-open/inventory-drop are still inherited from
 * {@link NukeCasingBlockBase} (its {@code onRemove} is already generalized to
 * {@link MachineBaseBlockEntity}, and {@code neighborChanged}'s default "{@code explode(level, pos,
 * null)} on redstone power" already matches CE's own {@code neighborChanged} body for this class
 * exactly).
 */
public class NukeBalefireBlock extends NukeCasingBlockBase {

    public NukeBalefireBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeBalefireBlockEntity(NukeCasingBlockEntities.NUKE_BALEFIRE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == NukeCasingBlockEntities.NUKE_BALEFIRE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NukeBalefireBlockEntity be) {
            be.placerID = serverPlayer.getUUID();
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof NukeBalefireBlockEntity be) || !be.isLoaded()) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        ModContext.DETONATOR_CONTEXT.set(detonator);
        try {
            be.explode();
        } finally {
            ModContext.DETONATOR_CONTEXT.remove();
        }
        return BombReturnCode.DETONATED;
    }
}
