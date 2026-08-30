package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * CE: {@code IFortuneMutator}. Lets a {@code BlockProcessorStandard} override the effective fortune
 * level used for a block's explosion drops. {@code int x, y, z} -&gt; {@link BlockPos} (see
 * {@link IDropChanceMutator}'s javadoc - same modernization, same reasoning).
 */
public interface IFortuneMutator {

    int mutateFortune(ExplosionVNT explosion, Block block, BlockPos pos);
}
