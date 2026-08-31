package com.hbm.items.special;

import static com.hbm.items.special.ProcessingTrait.ARC;
import static com.hbm.items.special.ProcessingTrait.CENTRIFUGED;
import static com.hbm.items.special.ProcessingTrait.RAD;
import static com.hbm.items.special.ProcessingTrait.ROASTED;
import static com.hbm.items.special.ProcessingTrait.SOLVENT;
import static com.hbm.items.special.ProcessingTrait.SULFURIC;
import static com.hbm.items.special.ProcessingTrait.WASHED;

/**
 * The 26 ore-slopper processing stages a {@link BedrockOreType} can be refined to, each carrying a
 * tint (CE recolored a shared base texture per grade) and the {@link ProcessingTrait}s baked into
 * its tooltip/overlay stack. Ported verbatim, in original declaration order, from CE's
 * {@code ItemBedrockOreNew.BedrockOreGrade}.
 * <p>
 * Confirmed dense: every {@link BedrockOreType} supports every one of these 26 grades (CE's
 * {@code getSubItems}/{@code registerModels}/{@code make()} all iterate the full
 * grade x type nested loop unconditionally, no combination is skipped) - the 6 x 26 = 156 cross
 * product is a genuine full grid, not sparse.
 */
public enum BedrockOreGrade {
    BASE(Tints.NONE, "base"),
    BASE_ROASTED(Tints.ROASTED, "base", ROASTED),
    BASE_WASHED(Tints.WASHED, "base", WASHED),
    PRIMARY(Tints.NONE, "primary", CENTRIFUGED),
    PRIMARY_ROASTED(Tints.ROASTED, "primary", ROASTED),
    PRIMARY_SULFURIC(0xFFFFD3, "primary", SULFURIC),
    PRIMARY_NOSULFURIC(0xD3D4FF, "primary", CENTRIFUGED, SULFURIC),
    PRIMARY_SOLVENT(0xD3F0FF, "primary", SOLVENT),
    PRIMARY_NOSOLVENT(0xFFDED3, "primary", CENTRIFUGED, SOLVENT),
    PRIMARY_RAD(0xECFFD3, "primary", RAD),
    PRIMARY_NORAD(0xEBD3FF, "primary", CENTRIFUGED, RAD),
    PRIMARY_FIRST(0xFFD3D4, "primary", CENTRIFUGED),
    PRIMARY_SECOND(0xD3FFEB, "primary", CENTRIFUGED),
    CRUMBS(Tints.NONE, "crumbs", CENTRIFUGED),

    SULFURIC_BYPRODUCT(Tints.NONE, "sulfuric", CENTRIFUGED, SULFURIC),
    SULFURIC_ROASTED(Tints.ROASTED, "sulfuric", ROASTED, SULFURIC),
    SULFURIC_ARC(Tints.ARC, "sulfuric", ARC, SULFURIC),
    SULFURIC_WASHED(Tints.WASHED, "sulfuric", WASHED, SULFURIC),

    SOLVENT_BYPRODUCT(Tints.NONE, "solvent", CENTRIFUGED, SOLVENT),
    SOLVENT_ROASTED(Tints.ROASTED, "solvent", ROASTED, SOLVENT),
    SOLVENT_ARC(Tints.ARC, "solvent", ARC, SOLVENT),
    SOLVENT_WASHED(Tints.WASHED, "solvent", WASHED, SOLVENT),

    RAD_BYPRODUCT(Tints.NONE, "rad", CENTRIFUGED, RAD),
    RAD_ROASTED(Tints.ROASTED, "rad", ROASTED, RAD),
    RAD_ARC(Tints.ARC, "rad", ARC, RAD),
    RAD_WASHED(Tints.WASHED, "rad", WASHED, RAD);

    public static final BedrockOreGrade[] VALUES = values();

    /**
     * Holder so enum constants can reference tints without an illegal forward reference
     * (enum ctors run before the enum class's own static fields).
     */
    private static final class Tints {
        static final int NONE = 0xFFFFFF;
        static final int ROASTED = 0xCFCFCF;
        static final int ARC = 0xC3A2A2;
        static final int WASHED = 0xDBE2CB;
    }

    public final int tint;
    public final String prefix;
    public final ProcessingTrait[] traits;

    BedrockOreGrade(int tint, String prefix, ProcessingTrait... traits) {
        this.tint = tint;
        this.prefix = prefix;
        this.traits = traits;
    }
}
