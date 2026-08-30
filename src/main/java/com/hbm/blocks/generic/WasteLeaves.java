package com.hbm.blocks.generic;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code WasteLeaves extends BlockOldLeaf}: contaminated leaves that never decay
 * or drop saplings/apples (CE overrode {@code updateTick}/{@code randomTick} as no-ops and zeroed
 * the sapling drop chance). Disabling random ticking entirely reproduces that - vanilla
 * {@link LeavesBlock}'s distance-based decay logic only ever runs from {@code randomTick}.
 * <p>
 * CE's bespoke chance-based drop table (25% dead bush, 33% stick, plus a distinct shears-only
 * drop of itself) is not ported - loot tables are a datagen concern out of this block-registration
 * pass's scope, matching the stance already taken for {@link WasteEarth}/{@link WasteSand}.
 */
public class WasteLeaves extends LeavesBlock {

    public WasteLeaves(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }
}
