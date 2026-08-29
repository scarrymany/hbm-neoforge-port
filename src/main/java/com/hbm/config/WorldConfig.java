package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's {@code WorldConfig}: bedrock ore toggles and spawn weights, limestone/hematite/
 * malachite/bauxite/cave toggles, the meteor system, and crater-biome radiation constants.
 * Registered into {@link HbmConfig}'s COMMON spec.
 * <p>
 * CE stored the crater-biome radiation constants as {@code float}; {@code ModConfigSpec} has no
 * {@code FloatValue}, so they're ported as {@link DoubleValue}s. Cast to {@code float} at the
 * point of use if a downstream API strictly requires it.
 */
public class WorldConfig {

    public static BooleanValue NEW_BEDROCK_ORES;
    public static IntValue LIMESTONE_SPAWN;

    public static BooleanValue ENABLE_HEMATITE;
    public static BooleanValue ENABLE_MALACHITE;
    public static BooleanValue ENABLE_BAUXITE;

    public static BooleanValue ENABLE_SULFUR_CAVE;
    public static BooleanValue ENABLE_ASBESTOS_CAVE;

    public static BooleanValue ENABLE_METEOR_STRIKES;
    public static BooleanValue ENABLE_METEOR_SHOWERS;
    public static BooleanValue ENABLE_METEOR_TAILS;
    public static BooleanValue ENABLE_SPECIAL_METEORS;
    public static IntValue METEOR_STRIKE_CHANCE;
    public static IntValue METEOR_SHOWER_CHANCE;
    public static IntValue METEOR_SHOWER_DURATION;

    public static BooleanValue ENABLE_CRATER_BIOMES;
    public static DoubleValue CRATER_BIOME_RAD;
    public static DoubleValue CRATER_BIOME_INNER_RAD;
    public static DoubleValue CRATER_BIOME_OUTER_RAD;
    public static DoubleValue CRATER_BIOME_WATER_MULT;

    public static IntValue BEDROCK_GLOWSTONE_SPAWN;
    public static IntValue BEDROCK_PHOSPHORUS_SPAWN;
    public static IntValue BEDROCK_QUARTZ_SPAWN;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("ores");

        NEW_BEDROCK_ORES = builder.comment("Enables the generation of bedrock ores. [CE: 2.NB_newBedrockOres]")
                .define("newBedrockOres", true);
        BEDROCK_GLOWSTONE_SPAWN = builder.comment("Spawn weight for glowstone bedrock ore. [CE: 2.BN00_bedrockGlowstoneWeight]")
                .defineInRange("bedrockGlowstoneWeight", 100, 0, Integer.MAX_VALUE);
        BEDROCK_PHOSPHORUS_SPAWN = builder.comment("Spawn weight for phosphorus bedrock ore. [CE: 2.BN01_bedrockPhosphorusWeight]")
                .defineInRange("bedrockPhosphorusWeight", 50, 0, Integer.MAX_VALUE);
        BEDROCK_QUARTZ_SPAWN = builder.comment("Spawn weight for quartz bedrock ore. [CE: 2.BN02_bedrockQuartzWeight]")
                .defineInRange("bedrockQuartzWeight", 100, 0, Integer.MAX_VALUE);
        LIMESTONE_SPAWN = builder.comment("Amount of limestone block veins per chunk. [CE: 2.L02_limestoneSpawn]")
                .defineInRange("limestoneSpawn", 1, 0, Integer.MAX_VALUE);

        ENABLE_HEMATITE = builder.comment("Toggles hematite deposits. [CE: 2.L00_enableHematite]").define("enableHematite", true);
        ENABLE_MALACHITE = builder.comment("Toggles malachite deposits. [CE: 2.L01_enableMalachite]").define("enableMalachite", true);
        ENABLE_BAUXITE = builder.comment("Toggles bauxite deposits. [CE: 2.L02_enableBauxite]").define("enableBauxite", true);

        ENABLE_SULFUR_CAVE = builder.comment("Toggles sulfur caves. [CE: 2.C00_enableSulfurCave]").define("enableSulfurCave", true);
        ENABLE_ASBESTOS_CAVE = builder.comment("Toggles asbestos caves. [CE: 2.C01_enableAsbestosCave]").define("enableAsbestosCave", true);

        builder.pop();

        builder.push("biomes");

        ENABLE_CRATER_BIOMES = builder.comment("Enables the biome change caused by nuclear explosions. [CE: 17.B_toggle]")
                .define("craterBiome", true);
        CRATER_BIOME_RAD = builder.comment("RAD/s for the crater biome. [CE: 17.R00_craterBiomeRad]")
                .defineInRange("craterBiomeRad", 5D, 0D, Double.MAX_VALUE);
        CRATER_BIOME_INNER_RAD = builder.comment("RAD/s for the inner crater biome. [CE: 17.R01_craterBiomeInnerRad]")
                .defineInRange("craterBiomeInnerRad", 25D, 0D, Double.MAX_VALUE);
        CRATER_BIOME_OUTER_RAD = builder.comment("RAD/s for the outer crater biome. [CE: 17.R02_craterBiomeOuterRad]")
                .defineInRange("craterBiomeOuterRad", 0.5D, 0D, Double.MAX_VALUE);
        CRATER_BIOME_WATER_MULT = builder.comment("Multiplier for RAD/s in crater biomes when in water. [CE: 17.R03_craterBiomeWaterMult]")
                .defineInRange("craterBiomeWaterMult", 5D, 0D, Double.MAX_VALUE);

        builder.pop();

        builder.push("meteors");

        ENABLE_METEOR_STRIKES = builder.comment("Toggles the spawning of meteors. [CE: 5.00_enableMeteorStrikes]").define("enableMeteorStrikes", true);
        ENABLE_METEOR_SHOWERS = builder.comment("Toggles meteor showers, which start with a 1% chance for every spawned meteor. [CE: 5.01_enableMeteorShowers]").define("enableMeteorShowers", true);
        ENABLE_METEOR_TAILS = builder.comment("Toggles the particle effect created by falling meteors. [CE: 5.02_enableMeteorTails]").define("enableMeteorTails", true);
        ENABLE_SPECIAL_METEORS = builder.comment("Toggles rare, special meteor types with different impact effects. [CE: 5.03_enableSpecialMeteors]").define("enableSpecialMeteors", true);
        METEOR_STRIKE_CHANCE = builder.comment("The probability of a meteor spawning (an average of once every nth tick). [CE: 5.03_meteorStrikeChance]")
                .defineInRange("meteorStrikeChance", 20 * 60 * 60 * 5, 1, Integer.MAX_VALUE);
        METEOR_SHOWER_CHANCE = builder.comment("The probability of a meteor spawning during a meteor shower (an average of once every nth tick). [CE: 5.04_meteorShowerChance]")
                .defineInRange("meteorShowerChance", 20 * 60 * 15, 1, Integer.MAX_VALUE);
        METEOR_SHOWER_DURATION = builder.comment("Max duration of a meteor shower, in ticks. [CE: 5.05_meteorShowerDuration]")
                .defineInRange("meteorShowerDuration", 20 * 60 * 30, 0, Integer.MAX_VALUE);

        builder.pop();
    }
}
