package com.hbm.items.special;

import com.hbm.inventory.material.NTMMaterial;

import static com.hbm.inventory.material.Mats.MAT_ASBESTOS;
import static com.hbm.inventory.material.Mats.MAT_BAUXITE;
import static com.hbm.inventory.material.Mats.MAT_BERYLLIUM;
import static com.hbm.inventory.material.Mats.MAT_BISMUTH;
import static com.hbm.inventory.material.Mats.MAT_BORAX;
import static com.hbm.inventory.material.Mats.MAT_BORON;
import static com.hbm.inventory.material.Mats.MAT_CHLOROCALCITE;
import static com.hbm.inventory.material.Mats.MAT_CINNABAR;
import static com.hbm.inventory.material.Mats.MAT_COAL;
import static com.hbm.inventory.material.Mats.MAT_COBALT;
import static com.hbm.inventory.material.Mats.MAT_COPPER;
import static com.hbm.inventory.material.Mats.MAT_CRYOLITE;
import static com.hbm.inventory.material.Mats.MAT_DIAMOND;
import static com.hbm.inventory.material.Mats.MAT_EMERALD;
import static com.hbm.inventory.material.Mats.MAT_FLUORITE;
import static com.hbm.inventory.material.Mats.MAT_GOLD;
import static com.hbm.inventory.material.Mats.MAT_IRON;
import static com.hbm.inventory.material.Mats.MAT_KNO;
import static com.hbm.inventory.material.Mats.MAT_LANTHANIUM;
import static com.hbm.inventory.material.Mats.MAT_LEAD;
import static com.hbm.inventory.material.Mats.MAT_LIGNITE;
import static com.hbm.inventory.material.Mats.MAT_LITHIUM;
import static com.hbm.inventory.material.Mats.MAT_MOLYSITE;
import static com.hbm.inventory.material.Mats.MAT_NEODYMIUM;
import static com.hbm.inventory.material.Mats.MAT_NIOBIUM;
import static com.hbm.inventory.material.Mats.MAT_PHOSPHORUS;
import static com.hbm.inventory.material.Mats.MAT_POLONIUM;
import static com.hbm.inventory.material.Mats.MAT_RADIUM;
import static com.hbm.inventory.material.Mats.MAT_RAREEARTH;
import static com.hbm.inventory.material.Mats.MAT_REDSTONE;
import static com.hbm.inventory.material.Mats.MAT_SILICON;
import static com.hbm.inventory.material.Mats.MAT_SODALITE;
import static com.hbm.inventory.material.Mats.MAT_SODIUM;
import static com.hbm.inventory.material.Mats.MAT_STRONTIUM;
import static com.hbm.inventory.material.Mats.MAT_SULFUR;
import static com.hbm.inventory.material.Mats.MAT_TANTALIUM;
import static com.hbm.inventory.material.Mats.MAT_TECHNETIUM;
import static com.hbm.inventory.material.Mats.MAT_THORIUM;
import static com.hbm.inventory.material.Mats.MAT_TITANIUM;
import static com.hbm.inventory.material.Mats.MAT_TUNGSTEN;
import static com.hbm.inventory.material.Mats.MAT_U238;
import static com.hbm.inventory.material.Mats.MAT_URANIUM;
import static com.hbm.inventory.material.Mats.MAT_ZIRCONIUM;

/**
 * The six ore-family buckets scanned by {@link ItemBedrockOreBase} and refined through
 * {@link BedrockOreGrade}'s processing chain. Ported verbatim (tints, suffixes, and the eleven
 * primary/byproduct output slots per type) from CE's {@code ItemBedrockOreNew.BedrockOreType}.
 * {@code light}/{@code dark} are the runtime recolor bounds CE applied to one shared grayscale
 * texture per type - not used by this port's registration, since per-variant texture/tint baking
 * belongs to the model/datagen redesign flagged in items_special.md finding 6, not to item
 * registration.
 */
public enum BedrockOreType {
    LIGHT_METAL(0xFFFFFF, 0x353535, "light",
            o(MAT_IRON, 9), o(MAT_COPPER, 9),
            o(MAT_TITANIUM, 6), o(MAT_BAUXITE, 9), o(MAT_CRYOLITE, 3),
            o(MAT_CHLOROCALCITE, 5), o(MAT_LITHIUM, 5), o(MAT_SODIUM, 3),
            o(MAT_CHLOROCALCITE, 6), o(MAT_LITHIUM, 6), o(MAT_SODIUM, 6)),
    HEAVY_METAL(0x868686, 0x000000, "heavy",
            o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9),
            o(MAT_GOLD, 2), o(MAT_GOLD, 2), o(MAT_BERYLLIUM, 3),
            o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9), o(MAT_GOLD, 5),
            o(MAT_BISMUTH, 2), o(MAT_TANTALIUM, 2), o(MAT_GOLD, 6)),
    RARE_EARTH(0xE6E6B6, 0x1C1C00, "rare",
            o(MAT_COBALT, 5), o(MAT_RAREEARTH, 5),
            o(MAT_BORON, 5), o(MAT_LANTHANIUM, 3), o(MAT_NIOBIUM, 4),
            o(MAT_NEODYMIUM, 3), o(MAT_STRONTIUM, 3), o(MAT_ZIRCONIUM, 3),
            o(MAT_NIOBIUM, 5), o(MAT_NEODYMIUM, 5), o(MAT_STRONTIUM, 3)),
    ACTINIDE(0xC1C7BD, 0x2B3227, "actinide",
            o(MAT_URANIUM, 4), o(MAT_THORIUM, 4),
            o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2),
            o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2),
            o(MAT_TECHNETIUM, 1), o(MAT_TECHNETIUM, 1), o(MAT_U238, 1)),
    NON_METAL(0xAFAFAF, 0x0F0F0F, "nonmetal",
            o(MAT_COAL, 9), o(MAT_SULFUR, 9),
            o(MAT_LIGNITE, 9), o(MAT_KNO, 6), o(MAT_FLUORITE, 6),
            o(MAT_PHOSPHORUS, 5), o(MAT_FLUORITE, 6), o(MAT_SULFUR, 6),
            o(MAT_CHLOROCALCITE, 6), o(MAT_SILICON, 2), o(MAT_SILICON, 2)),
    CRYSTALLINE(0xE2FFFA, 0x1E8A77, "crystal",
            o(MAT_REDSTONE, 9), o(MAT_CINNABAR, 4),
            o(MAT_SODALITE, 9), o(MAT_ASBESTOS, 6), o(MAT_DIAMOND, 3),
            o(MAT_CINNABAR, 3), o(MAT_ASBESTOS, 5), o(MAT_EMERALD, 3),
            o(MAT_BORAX, 3), o(MAT_MOLYSITE, 3), o(MAT_SODALITE, 9));

    public static final BedrockOreType[] VALUES = values();

    public final int light;
    public final int dark;
    public final String suffix;
    public final BedrockOreOutput primary1;
    public final BedrockOreOutput primary2;
    public final BedrockOreOutput byproductAcid1;
    public final BedrockOreOutput byproductAcid2;
    public final BedrockOreOutput byproductAcid3;
    public final BedrockOreOutput byproductSolvent1;
    public final BedrockOreOutput byproductSolvent2;
    public final BedrockOreOutput byproductSolvent3;
    public final BedrockOreOutput byproductRad1;
    public final BedrockOreOutput byproductRad2;
    public final BedrockOreOutput byproductRad3;

    BedrockOreType(int light, int dark, String suffix,
                   BedrockOreOutput p1, BedrockOreOutput p2,
                   BedrockOreOutput bAcid1, BedrockOreOutput bAcid2, BedrockOreOutput bAcid3,
                   BedrockOreOutput bSolvent1, BedrockOreOutput bSolvent2, BedrockOreOutput bSolvent3,
                   BedrockOreOutput bRad1, BedrockOreOutput bRad2, BedrockOreOutput bRad3) {
        this.light = light;
        this.dark = dark;
        this.suffix = suffix;
        this.primary1 = p1;
        this.primary2 = p2;
        this.byproductAcid1 = bAcid1;
        this.byproductAcid2 = bAcid2;
        this.byproductAcid3 = bAcid3;
        this.byproductSolvent1 = bSolvent1;
        this.byproductSolvent2 = bSolvent2;
        this.byproductSolvent3 = bSolvent3;
        this.byproductRad1 = bRad1;
        this.byproductRad2 = bRad2;
        this.byproductRad3 = bRad3;
    }

    private static BedrockOreOutput o(NTMMaterial mat, int amount) {
        return new BedrockOreOutput(mat, amount);
    }
}
