package com.hbm.blocks.generic;

/**
 * Ported from CE's {@code BlockOutgas extends BlockNTMOre} ({@code ore_uranium}, its scorched/
 * gneiss/nether siblings, {@code block_asbestos}): on top of the ordinary ore self-drop, CE randomly
 * placed a radon/asbestos-dust gas block above the ore on tick, on break, and (for one call site) on
 * a neighbor update.
 * <p>
 * Every one of CE's {@code BlockOutgas} call sites in this area passes no {@link com.hbm.blocks.IOreType}
 * (harvest level 1, plain self-drop), so the only thing this subclass actually changes for Phase 1 is
 * the class identity - the gas-emission behavior itself depends on the gas-block family and the
 * random/neighbor tick scheduling CE drove it with, neither of which exist in the port yet. Rather
 * than carry unused rate/onBreak/onNeighbour fields with no implementation behind them, this class is
 * kept a plain, faithful placeholder for the future gas-system pass to extend; it still gives every
 * uranium-family ore and {@code block_asbestos} their own type distinct from plain {@link BlockNTMOre}
 * so that future work can find them via {@code instanceof BlockOutgas}.
 */
public class BlockOutgas extends BlockNTMOre {

    public BlockOutgas(Properties properties) {
        super(properties, null);
    }
}
