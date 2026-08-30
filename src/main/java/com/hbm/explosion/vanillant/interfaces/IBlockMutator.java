package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: {@code IBlockMutator}. A pre/post block-conversion hook run by {@code BlockProcessorStandard}
 * around the actual removal of each affected block - {@code mutatePre} sees the original
 * {@link BlockState} just before removal, {@code mutatePost} runs afterward for every position that
 * ended up air (e.g. {@code BlockMutatorFire} igniting the crater rim). {@code IBlockState} -&gt;
 * {@link BlockState}, otherwise identical to CE's shape.
 */
public interface IBlockMutator {

    void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos);

    void mutatePost(ExplosionVNT explosion, BlockPos pos);
}
