package com.hbm.items.special;

import com.hbm.items.ItemBase;

/**
 * Port of CE's {@code ItemNuclearWaste}: base class for waste items that, when dropped, spawn as a
 * custom {@code EntityItemWaste} (infinite despawn timer, longer pickup delay) instead of a plain
 * {@code ItemEntity}. No entity system has been ported through Phase 1 (see
 * docs/phase1/items_special.md finding 4's sibling systems), so the
 * {@code hasCustomEntity}/{@code createEntity}/{@code getEntityLifespan} hooks
 * ({@code IItemExtension} on modern {@code Item}, confirmed against the Neo Edition reference and
 * this project's own decompiled 1.21.1 sources) are left at their {@code Item} defaults rather than
 * spawning an entity class that does not exist yet. {@link ItemDepletedFuel} extends this directly,
 * matching CE's class hierarchy.
 */
public class ItemNuclearWaste extends ItemBase {

    public ItemNuclearWaste(Properties properties) {
        super(properties);
    }
}
