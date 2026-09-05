package com.hbm.blocks.machine.rbmk;

import com.mojang.serialization.MapCodec;

/**
 * CE: rbmk_display_blank - base panel block, no TE, no GUI. Just a decorative panel.
 * Used as crafting ingredient for display/gauge/keypad/numitron/graph/lever/indicator/terminal.
 * CE: RBMKMiniPanelBase("rbmk_display_blank"), no createNewTileEntity override (returns null).
 */
public class RBMKDisplayBlankBlock extends RBMKMiniPanelBlock {
    public static final MapCodec<RBMKDisplayBlankBlock> CODEC = simpleCodec(RBMKDisplayBlankBlock::new);

    public RBMKDisplayBlankBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<RBMKDisplayBlankBlock> codec() {
        return CODEC;
    }
}
