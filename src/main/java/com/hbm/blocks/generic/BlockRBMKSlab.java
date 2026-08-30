package com.hbm.blocks.generic;

/**
 * RBMK reactor core visual slab, ported from CE's {@code BlockRBMKSlab}. CE hand-rolled a
 * single/double slab pair (a 1.12-era necessity, see {@link BlockGenericSlab}'s own javadoc) purely
 * to get vanilla-slab-shaped placement/combination behavior for two decorative RBMK panel
 * textures - vanilla's own slab block already provides that behavior natively, so this is infrastructure
 * only, exactly like {@link BlockGenericSlab}: a distinctly-named subclass for the future RBMK
 * multiblock content (per the port report, "decorative shell for the future RBMK multiblock", Phase
 * 2) to build on, with no instances registered from this package since the RBMK panel *block* this
 * slab type is meant to pair with is itself Phase 2/out of this survey's scope.
 */
public class BlockRBMKSlab extends BlockGenericSlab {

    public BlockRBMKSlab(Properties properties) {
        super(properties);
    }
}
