package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of CE's {@code PotionConfig}. Registered into {@link HbmConfig}'s COMMON spec.
 * <p>
 * CE stored {@code potionSickness} as a free-text string ("OFF"/"NORMAL"/"TERRARIA") parsed into
 * an int mode. Ported as a validated enumerated string value instead, since ModConfigSpec can
 * restrict a string to a fixed set of allowed values directly.
 */
public class PotionConfig {

    public enum SicknessMode { OFF, NORMAL, TERRARIA }

    public static ConfigValue<List<? extends String>> POTION_BLACKLIST_RAW;
    public static BooleanValue DO_JUMP_BOOST;
    public static ConfigValue<String> POTION_SICKNESS;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("potions");

        POTION_BLACKLIST_RAW = builder
                .comment("Potions that get blocked while wearing a hazmat suit with bacteria protection. [CE: 08.01_hazmatPotionBlacklist]")
                .defineListAllowEmpty("hazmatPotionBlacklist", () -> List.of(
                        "srparasites:coth",
                        "srparasites:viral"
                ), entry -> entry instanceof String);
        DO_JUMP_BOOST = builder
                .comment("Whether Servos and Armors should give Jump Boost. [CE: 8.02_doJumpBoost]")
                .define("doJumpBoost", true);
        // ArrayList, not List.of: NeoForge defineInList uses acceptable::contains and
        // ValueSpec.test(null) on missing keys. Java 21 List.of().contains(null) NPEs.
        POTION_SICKNESS = builder
                .comment("Valid values are OFF, NORMAL and TERRARIA. [CE: 8.03_potionSickness]")
                .defineInList("potionSickness", SicknessMode.OFF.name(), new ArrayList<>(List.of(
                        SicknessMode.OFF.name(), SicknessMode.NORMAL.name(), SicknessMode.TERRARIA.name()
                )));

        builder.pop();
    }

    public static Set<String> potionBlacklist() {
        return new HashSet<>(POTION_BLACKLIST_RAW.get());
    }

    public static SicknessMode potionSicknessMode() {
        try {
            return SicknessMode.valueOf(POTION_SICKNESS.get().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SicknessMode.OFF;
        }
    }
}
