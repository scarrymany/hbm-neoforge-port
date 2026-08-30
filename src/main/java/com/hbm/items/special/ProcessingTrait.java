package com.hbm.items.special;

/**
 * Processing steps a {@link BedrockOreGrade} has been through (ore-slopper chain flavor text and
 * texture-overlay layers in CE). Ported verbatim from CE's
 * {@code ItemBedrockOreNew.ProcessingTrait}.
 */
public enum ProcessingTrait {
    ROASTED,
    ARC,
    WASHED,
    CENTRIFUGED,
    SULFURIC,
    SOLVENT,
    RAD;

    public static final ProcessingTrait[] VALUES = values();
}
