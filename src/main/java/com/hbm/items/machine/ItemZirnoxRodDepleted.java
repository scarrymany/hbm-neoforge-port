package com.hbm.items.machine;

import com.hbm.items.ItemBase;

/**
 * Depleted Zirnox rod byproduct: inert marker item, zero logic beyond the shared enum. CE's nine
 * metadata variants become nine registered instances.
 */
public class ItemZirnoxRodDepleted extends ItemBase {

    private final EnumZirnoxTypeDepleted type;

    public ItemZirnoxRodDepleted(EnumZirnoxTypeDepleted type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EnumZirnoxTypeDepleted getType() {
        return this.type;
    }

    public enum EnumZirnoxTypeDepleted {
        NATURAL_URANIUM_FUEL,
        URANIUM_FUEL,
        THORIUM_FUEL,
        MOX_FUEL,
        PLUTONIUM_FUEL,
        U233_FUEL,
        U235_FUEL,
        LES_FUEL,
        ZFB_MOX_FUEL;

        public static final EnumZirnoxTypeDepleted[] VALUES = values();
    }
}
