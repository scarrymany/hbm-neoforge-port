package com.hbm.blocks.network.energy;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.BlockCableClassic} - a trivial
 * texture-only {@link BlockCable} subclass (CE's own class body is nothing but an alternate
 * {@code bakeModel} override, out of scope here per this port's block/rendering split). Registered
 * separately ({@code red_cable_classic}) purely so it gets its own {@code BlockItem}/creative-tab
 * entry and texture.
 */
public class BlockCableClassic extends BlockCable {

    public BlockCableClassic(Properties properties) {
        super(properties);
    }
}
