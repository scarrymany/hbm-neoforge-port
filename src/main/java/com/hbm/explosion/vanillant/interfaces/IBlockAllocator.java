package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;

/**
 * CE: {@code com.hbm.explosion.vanillant.interfaces.IBlockAllocator}. Decides which {@link BlockPos}
 * are affected by an {@link ExplosionVNT} - {@code World} -&gt; {@link Level}, otherwise identical to
 * CE's shape (confirmed against Neo Edition's real, compiling 1.21.1 port of the same interface).
 */
public interface IBlockAllocator {

    HashSet<BlockPos> allocate(ExplosionVNT explosion, Level level, double x, double y, double z, float size);
}
