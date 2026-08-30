package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;

/**
 * Special-purpose concrete finishes (machine floor stripes, hazard stripes, and a handful of extra
 * colors vanilla dye doesn't cover), ported from CE's {@code BlockConcreteColoredExt}. CE folded
 * eight textures into one {@code BlockEnumMeta<EnumConcreteType>} instance and additionally forced
 * {@code canCreatureSpawn} to always return {@code false} with a matching tooltip line - exactly the
 * behavior {@link BlockBase}'s {@code noSpawn} constructor flag already reproduces, so this class
 * only needs to supply the flattened {@link Type} field on top of it (per this port's flattening
 * rule, one registry entry per {@link Type} constant - see {@link GenericBlocks}).
 */
public class BlockConcreteColoredExt extends BlockBase {

    public enum Type {
        MACHINE,
        MACHINE_STRIPE,
        INDIGO,
        PURPLE,
        PINK,
        HAZARD,
        SAND,
        BRONZE;

        public static final Type[] VALUES = values();
    }

    public final Type type;

    public BlockConcreteColoredExt(Properties properties, Type type) {
        super(properties, true);
        this.type = type;
    }
}
