package com.hbm.items.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.items.tool.IItemAbility}. CE's own report on this interface
 * still holds true after re-checking this port's {@link ItemToolAbility}: the real
 * break-extra-block logic lives as a same-named concrete instance method
 * ({@link ItemToolAbility#breakExtraBlock}), not an override of this interface - nothing in this
 * port implements {@code IItemAbility} either. Kept only for CE parity/cross-referencing.
 */
public interface IItemAbility {

    void breakExtraBlock(Level world, BlockPos pos, Player player, BlockPos refPos, InteractionHand hand);
}
