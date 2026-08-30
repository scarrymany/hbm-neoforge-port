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
    BASE(TINT_NONE, "base"),
    BASE_ROASTED(TINT_ROASTED, "base", ROASTED),
    BASE_WASHED(TINT_WASHED, "base", WASHED),
    PRIMARY(TINT_NONE, "primary", CENTRIFUGED),
    PRIMARY_ROASTED(TINT_ROASTED, "primary", ROASTED),
    PRIMARY_SULFURIC(0xFFFFD3, "primary", SULFURIC),
    PRIMARY_NOSULFURIC(0xD3D4FF, "primary", CENTRIFUGED, SULFURIC),
    PRIMARY_SOLVENT(0xD3F0FF, "primary", SOLVENT),
    PRIMARY_NOSOLVENT(0xFFDED3, "primary", CENTRIFUGED, SOLVENT),
    PRIMARY_RAD(0xECFFD3, "primary", RAD),
    PRIMARY_NORAD(0xEBD3FF, "primary", CENTRIFUGED, RAD),
    PRIMARY_FIRST(0xFFD3D4, "primary", CENTRIFUGED),
    PRIMARY_SECOND(0xD3FFEB, "primary", CENTRIFUGED),
    CRUMBS(TINT_NONE, "crumbs", CENTRIFUGED),

    SULFURIC_BYPRODUCT(TINT_NONE, "sulfuric", CENTRIFUGED, SULFURIC),
    SULFURIC_ROASTED(TINT_ROASTED, "sulfuric", ROASTED, SULFURIC),
    SULFURIC_ARC(TINT_ARC, "sulfuric", ARC, SULFURIC),
    SULFURIC_WASHED(TINT_WASHED, "sulfuric", WASHED, SULFURIC),

    SOLVENT_BYPRODUCT(TINT_NONE, "solvent", CENTRIFUGED, SOLVENT),
    SOLVENT_ROASTED(TINT_ROASTED, "solvent", ROASTED, SOLVENT),
    SOLVENT_ARC(TINT_ARC, "solvent", ARC, SOLVENT),
    SOLVENT_WASHED(TINT_WASHED, "solvent", WASHED, SOLVENT),

    RAD_BYPRODUCT(TINT_NONE, "rad", CENTRIFUGED, RAD),
    RAD_ROASTED(TINT_ROASTED, "rad", ROASTED, RAD),
    RAD_ARC(TINT_ARC, "rad", ARC, RAD),
    RAD_WASHED(TINT_WASHED, "rad", WASHED, RAD);

    public static final BedrockOreGrade[] VALUES = values();

    private static final int TINT_NONE = 0xFFFFFF;
    private static final int TINT_ROASTED = 0xCFCFCF;
    private static final int TINT_ARC = 0xC3A2A2;
    private static final int TINT_WASHED = 0xDBE2CB;

    public final int tint;
    public final String prefix;
    public final ProcessingTrait[] traits;

    BedrockOreGrade(int tint, String prefix, ProcessingTrait... traits) {
        this.tint = tint;
        this.prefix = prefix;
        this.traits = traits;
    }
}
