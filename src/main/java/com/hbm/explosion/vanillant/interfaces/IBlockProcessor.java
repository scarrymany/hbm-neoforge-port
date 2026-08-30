package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;

/**
 * CE: {@code IBlockProcessor}. Decides what happens to each block an {@link IBlockAllocator} marked
 * affected (drops, block-exploded notification, {@link IBlockMutator} hooks, and - in this port's
 * {@code BlockProcessorStandard} - the actual chunk-batched removal write). Signature otherwise
 * identical to CE's ({@code World} -&gt; {@link Level}).
 */
public interface IBlockProcessor {

    void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashSet<BlockPos> affectedBlocks);
}
