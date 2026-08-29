package com.hbm.blocks;

/**
 * CE used this to drive metadata-multiplexed block families (one {@code Block} instance covering
 * many variants via {@code ItemStack} damage/metadata, with {@code getTranslationKey(ItemStack)} and
 * {@code getOverrideDisplayName(ItemStack)} picking the right name per variant). Minecraft has had
 * no block metadata since 1.13: each variant is now its own registered {@code Block}/{@code BlockItem}
 * pair, so those ItemStack-driven naming hooks have no modern equivalent and are not ported.
 * <p>
 * What survives is the plain "how many variants does this enum-driven family have" bookkeeping,
 * kept in case a later phase still wants to index into a variant enum (e.g. for world generation
 * or loot tables) without resurrecting metadata multiplexing on the block itself.
 */
public interface IBlockMulti {

    int getSubCount();

    /**
     * Ported verbatim from CE's {@code Math.abs(meta % getSubCount())}, not
     * {@code Math.floorMod(index, getSubCount())}. The two differ for negative inputs
     * (e.g. subCount=5, index=-1: CE's formula gives 1, floorMod gives 4); CE's exact
     * wraparound is kept so tools that cycle backward through variants match CE behavior.
     */
    default int rectify(int index) {
        return Math.abs(index % getSubCount());
    }
}
