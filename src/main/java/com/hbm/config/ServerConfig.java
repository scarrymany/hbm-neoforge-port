package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's runtime-editable {@code ServerConfig} (server-authoritative balance values,
 * originally backed by CE's {@code RunningConfig}/{@code ConfigWrapper} and editable in-game via
 * {@code /ntmserver}). Registered into {@link HbmConfig}'s SERVER spec, so - unlike the COMMON
 * classes - these values are synced from the server to every connecting client.
 * <p>
 * See {@link ClientConfig}'s javadoc for the same deliberate feature-reduction note: CE's
 * {@code /ntmserver} live-edit command layer is dropped in favor of the standard
 * {@code ModConfigSpec} TOML file, matching the Neo Edition reference's own precedent.
 */
public class ServerConfig {

    public static BooleanValue DAMAGE_COMPATIBILITY_MODE;
    public static DoubleValue MINE_AP_DAMAGE;
    public static DoubleValue MINE_HE_DAMAGE;
    public static DoubleValue MINE_SHRAP_DAMAGE;
    public static DoubleValue MINE_NUKE_DAMAGE;
    public static DoubleValue MINE_NAVAL_DAMAGE;
    public static BooleanValue TAINT_TRAILS;
    public static BooleanValue CRATE_OPEN_HELD;
    public static BooleanValue CRATE_KEEP_CONTENTS;
    public static IntValue ITEM_HAZARD_DROP_TICKRATE;
    public static BooleanValue ENABLE_MKU;
    public static BooleanValue LEGACY_CRUCIBLE_RULES;
    public static IntValue AUTOCAL_MAX_CLOCK;
    public static IntValue CONVEYOR_CRAM_MAX;
    public static BooleanValue CONVEYOR_CRAM_EXPLODE;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("balance");

        DAMAGE_COMPATIBILITY_MODE = builder.comment("Uses vanilla-compatible damage sources/calculations instead of NTM's own, for better compatibility with other damage-modifying mods.")
                .define("damageCompatibilityMode", false);
        MINE_AP_DAMAGE = builder.comment("Damage dealt by AP landmines.").defineInRange("mineApDamage", 10D, 0D, Double.MAX_VALUE);
        MINE_HE_DAMAGE = builder.comment("Damage dealt by HE landmines.").defineInRange("mineHeDamage", 35D, 0D, Double.MAX_VALUE);
        MINE_SHRAP_DAMAGE = builder.comment("Damage dealt by shrapnel landmines.").defineInRange("mineShrapDamage", 7.5D, 0D, Double.MAX_VALUE);
        MINE_NUKE_DAMAGE = builder.comment("Damage dealt by nuclear landmines.").defineInRange("mineNukeDamage", 100D, 0D, Double.MAX_VALUE);
        MINE_NAVAL_DAMAGE = builder.comment("Damage dealt by naval mines.").defineInRange("mineNavalDamage", 60D, 0D, Double.MAX_VALUE);
        TAINT_TRAILS = builder.comment("Toggles Thaumcraft taint trail spreading behavior compatibility.").define("taintTrails", false);
        CRATE_OPEN_HELD = builder.comment("Allows opening crates while held in hand instead of only when placed.").define("crateOpenHeld", true);
        CRATE_KEEP_CONTENTS = builder.comment("Keeps crate contents when broken instead of dropping them loose.").define("crateKeepContents", true);
        ITEM_HAZARD_DROP_TICKRATE = builder.comment("Ticks between hazard checks for items lying on the ground.")
                .defineInRange("itemHazardDropTickrate", 2, 1, Integer.MAX_VALUE);
        ENABLE_MKU = builder.comment("Enables the MKU (upgrade module) system.").define("enableMku", true);
        LEGACY_CRUCIBLE_RULES = builder.comment("Uses the older, pre-rework crucible smelting rules.").define("legacyCrucibleRules", false);
        AUTOCAL_MAX_CLOCK = builder.comment("Maximum clock speed multiplier the autocalibrator can apply.")
                .defineInRange("autocalMaxClock", 20, 1, Integer.MAX_VALUE);
        CONVEYOR_CRAM_MAX = builder.comment("Maximum number of items a conveyor belt tile can be crammed with before overflow handling kicks in.")
                .defineInRange("conveyorCramMax", 25, 1, Integer.MAX_VALUE);
        CONVEYOR_CRAM_EXPLODE = builder.comment("Whether an over-crammed conveyor belt explodes instead of just refusing new items.").define("conveyorCramExplode", true);

        builder.pop();
    }
}
