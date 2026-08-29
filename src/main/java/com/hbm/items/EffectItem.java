package com.hbm.items;

/**
 * Marker subclass of ItemBase for items that are not obtainable in-game (dummy effect-display
 * items). Excluding these from creative tabs / JEI-equivalent lookups is handled by whoever
 * populates the creative tab contents, not by the item class itself.
 */
public class EffectItem extends ItemBase {

    public EffectItem(Properties properties) {
        super(properties);
    }
}
