package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;

/**
 * Ported from CE's {@code BlockRadResistant}: a marker for blocks CE's {@code RadiationSystemNT}
 * treats as radiation shielding (e.g. {@code block_lead}, {@code block_niter_reinforced}). That
 * chunk-radiation-propagation system is not ported yet, so this class currently carries no
 * behavioral difference from {@link BlockBase} - it exists so a future radiation-shielding pass can
 * find every shielding block via {@code instanceof BlockRadResistant} instead of re-deriving the set
 * from scratch, and so this family keeps its own registry-name/creative-tab identity matching CE.
 */
public class BlockRadResistant extends BlockBase {

    public BlockRadResistant(Properties properties) {
        super(properties);
    }
}
