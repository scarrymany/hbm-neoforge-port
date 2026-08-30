package com.hbm.items.special;

import net.minecraft.world.item.Item;

/**
 * Port of CE's {@code ItemStarterKit} - registration only, per docs/phase1/items_special.md's
 * explicit guidance. CE's {@code onItemRightClick} giveaway lists reference nuke machines/blocks
 * (Phase 2), hazmat/power armor (Phase 3), missiles and grenades (Phase 3) almost exhaustively
 * across the whole mod. Registers as a plain, single-stack item for now; the giveaway behavior
 * cannot be completed faithfully until those dependencies exist, and should be built up
 * incrementally per kit rather than guessed at here.
 */
public class ItemStarterKit extends Item {

    public ItemStarterKit(Properties properties) {
        super(properties);
    }
}
