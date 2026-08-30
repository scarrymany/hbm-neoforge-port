package com.hbm.items.machine;

import com.hbm.items.ItemBase;

/**
 * Pure marker item consumed by the (Phase 2) breeder block - zero logic of its own in CE. The
 * breeding material dimension ({@link BreedingRodType}, 17 values) and the physical multiplicity
 * dimension ({@link Multiplicity}, CE's separate {@code rod}/{@code rod_dual}/{@code rod_quad}
 * items) both used to be metadata; each (multiplicity, type) pair is now its own registered item.
 */
public class ItemBreedingRod extends ItemBase {

    private final Multiplicity multiplicity;
    private final BreedingRodType type;

    public ItemBreedingRod(Multiplicity multiplicity, BreedingRodType type, Properties properties) {
        super(properties);
        this.multiplicity = multiplicity;
        this.type = type;
    }

    public Multiplicity getMultiplicity() {
        return this.multiplicity;
    }

    public BreedingRodType getType() {
        return this.type;
    }

    public enum Multiplicity {
        SINGLE, DUAL, QUAD
    }

    public enum BreedingRodType {
        LITHIUM,
        TRITIUM,
        CO,
        CO60,
        TH232,
        THF,
        U235,
        NP237,
        U238,
        PU238,
        PU239,
        RGP,
        WASTE,

        //Required for prototype
        LEAD,
        URANIUM,

        RA226,
        AC227;

        public static final BreedingRodType[] VALUES = values();
    }
}
