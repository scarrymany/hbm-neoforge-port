package com.hbm.items.machine;

import com.hbm.items.ItemBase;

/**
 * Plain crafting-ingredient gear. CE modeled bronze/steel as a hardcoded metadata index on one
 * registry entry; each grade is its own registered item here. The CE {@code IMetaItemTesr}
 * TESR-on-item render binding is a client rendering detail out of this area's scope.
 */
public class ItemGear extends ItemBase {

    private final GearType type;

    public ItemGear(GearType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public GearType getType() {
        return this.type;
    }

    public enum GearType {
        BRONZE, STEEL;

        public static final GearType[] VALUES = values();
    }
}
