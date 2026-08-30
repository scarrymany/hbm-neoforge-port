package com.hbm.blockentity.network;

import net.minecraft.world.level.BlockGetter;

/**
 * Render-facing connection-mask cache contract, ported unchanged from CE's
 * {@code com.hbm.tileentity.network.ICachedPipeConnections} ({@code IBlockAccess} -&gt;
 * {@code BlockGetter}, the same substitution used throughout this port's {@code lib}/{@code api}
 * classes). Every duct/pipe block entity in this package implements it so a neighbor block-update can
 * invalidate the cache without knowing which concrete class it's talking to (see
 * {@link com.hbm.blocks.network.FluidDuctBaseBlock}'s {@code neighborChanged}/{@code onNeighborChange}
 * pair, mirroring CE's own {@code FluidDuctStandard}/{@code FluidDuctBox}).
 */
public interface ICachedPipeConnections {

    byte getCachedConnectionMask(BlockGetter access);

    void invalidateConnectionCache();
}
