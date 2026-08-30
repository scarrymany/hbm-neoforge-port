package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * CE: {@code IDropChanceMutator}. Lets a {@code BlockProcessorStandard} override the per-block item
 * drop chance. CE took separate {@code int x, y, z}; this port takes the already-available
 * {@link BlockPos} instead (a pure API-shape modernization - confirmed against Neo Edition's real
 * 1.21.1 port of the same interface - the value CE's callers compute from it is unchanged).
 */
public interface IDropChanceMutator {

    float mutateDropChance(ExplosionVNT explosion, Block block, BlockPos pos, float chance);
}
