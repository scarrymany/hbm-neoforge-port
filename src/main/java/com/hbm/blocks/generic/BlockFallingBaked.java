package com.hbm.blocks.generic;

import com.hbm.blocks.BlockFallingBase;

/**
 * Falling variant of the baked-block family, ported from CE's {@code BlockFallingBaked}. CE
 * hand-rolls its own fall-check/spawn-{@code EntityFallingBlock} logic, reproducing vanilla's own
 * {@code BlockFalling}/{@code FallingBlock} mechanic verbatim (same fall-through-air/water/lava/fire
 * check, same instant-vs-entity fall split); the port's existing {@link BlockFallingBase} already
 * wraps that exact vanilla behavior, so this class is reduced to a thin marker subclass rather than
 * re-implementing what {@link net.minecraft.world.level.block.FallingBlock} already provides. CE's
 * runtime model baking ({@code BlockBakeBase}) is replaced by the port's datagen ground rule, not
 * carried over.
 */
public class BlockFallingBaked extends BlockFallingBase {

    public BlockFallingBaked(Properties properties) {
        super(properties);
    }
}
