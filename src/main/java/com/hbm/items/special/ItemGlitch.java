package com.hbm.items.special;

import net.minecraft.world.item.Item;

/**
 * Port of CE's {@code ItemGlitch} ({@code glitch}) - registration only, per
 * docs/phase1/items_special.md's explicit guidance. CE's {@code onItemRightClick} rolls a 31-case
 * random-effect table referencing dozens of items/blocks/entities spanning missiles, nukes, treasure
 * blocks and potions across every future phase; implementing it faithfully needs those dependencies
 * to exist first. Registers as a plain, damageable, single-stack item for now; the effect table is
 * left for incremental completion once its target items/blocks/entities land.
 */
public class ItemGlitch extends Item {

    public ItemGlitch(Properties properties) {
        super(properties);
    }
}
