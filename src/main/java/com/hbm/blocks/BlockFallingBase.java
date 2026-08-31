package com.hbm.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;

/**
 * Generic falling-block base, replacing CE's {@code BlockFallingBase}. Registry name, translation
 * key, creative tab and harvest level are no longer block-instance concerns (see {@link BlockBase}
 * for the full rationale) so this class is reduced to a thin, Properties-driven {@link FallingBlock}
 * subclass.
 * <p>
 * CE hardcoded two joke tooltips here (for {@code gravel_diamond} and {@code sand_boron}) by
 * identity-checking {@code this} against {@code ModBlocks} fields. That content-specific tooltip
 * logic does not belong on a shared base class and is deferred to those two concrete block classes
 * once they are ported in a later phase.
 */
public class BlockFallingBase extends FallingBlock {

    public static final MapCodec<BlockFallingBase> CODEC = simpleCodec(BlockFallingBase::new);

    public BlockFallingBase(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BlockFallingBase> codec() {
        return CODEC;
    }
}
