package com.hbm.blocks.machine.rbmk;

/**
 * Shared base for the 5 CE control-rod block variants ({@code rbmk_control}/{@code _auto}/
 * {@code _mod}/{@code _reasim}/{@code _reasim_auto}) - the {@code moderated}/{@code reasimPowered}
 * flags replace CE's per-field {@code ModBlocks} identity checks
 * ({@code RBMKControl.moderated}/{@code TileEntityRBMKControl.isPowered()}'s block-identity switch).
 */
public abstract class RBMKControlBlock extends RBMKBaseBlock {

    public final boolean moderated;
    public final boolean reasimPowered;

    protected RBMKControlBlock(Properties properties, boolean moderated, boolean reasimPowered) {
        super(properties);
        this.moderated = moderated;
        this.reasimPowered = reasimPowered;
    }
}
