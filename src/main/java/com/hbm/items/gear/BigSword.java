package com.hbm.items.gear;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * Port of CE's {@code BigSword} - a plain {@code ItemSword} subclass with no overrides beyond its
 * constructor. Kept as a thin named subclass (rather than registering {@link SwordItem} directly)
 * purely to preserve CE's 1:1 file-per-class mapping for future cross-referencing.
 */
public class BigSword extends SwordItem {

    public BigSword(Tier tier, Properties properties) {
        super(tier, properties);
    }
}
