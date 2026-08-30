package com.hbm.items.tool;

import com.hbm.api.block.ILockable;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Installs a lock on an {@link ILockable} target, ported from CE's
 * {@code com.hbm.items.tool.ItemLock} (read in full). See {@link ItemKeyPin}'s javadoc for the
 * family-wide note on {@link ILockable} being real, generic, not-yet-consumed infrastructure.
 */
public class ItemLock extends ItemKeyPin {

    /** CE: per-instance {@code lockMod} constructor parameter - the pick-difficulty this lock installs. */
    private final double lockMod;

    public ItemLock(double lockMod, Properties properties) {
        super(properties);
        this.lockMod = lockMod;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide || getPins(stack) == 0) return InteractionResult.PASS;

        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core == null) return InteractionResult.FAIL;
            pos = core;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof ILockable lockable)) return InteractionResult.PASS;

        if (lockable.isLocked() || (player != null && !lockable.canLock(player, context.getHand(), context.getClickedFace()))) {
            return InteractionResult.FAIL;
        }

        lockable.setPins(getPins(stack));
        lockable.lock();
        lockable.setMod(this.lockMod);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
