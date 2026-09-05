package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Fuel rod channel column. Ported from CE's {@code RBMKRod} block class.
 * Held-rod insert Exact CE {@code RBMKRod.java:58-64} ({@code upgradePlug} 1.0F/1.0F BLOCKS).
 * {@code BossSpawnHandler.markFBI} stay skipped (deferred, same as research/ZIRNOX).
 */
public class RBMKRodBlock extends RBMKBaseBlock {

    public final boolean moderated;

    public RBMKRodBlock(Properties properties, boolean moderated) {
        super(properties);
        this.moderated = moderated;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new RBMKRodBlockEntity(RBMKBlockEntities.ROD.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RBMKBlockEntities.ROD.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemRBMKRod) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            BlockPos core = findCore(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof RBMKRodBlockEntity rbmk
                    && rbmk.inventory.getStackInSlot(0).isEmpty()) {
                ItemStack rod = stack.copy();
                rod.setCount(1);
                rbmk.inventory.setStackInSlot(0, rod);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                // Exact CE RBMKRod.java:63
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
