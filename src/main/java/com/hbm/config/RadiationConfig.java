package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's {@code RadiationConfig}: fallout rain/fog thresholds, world-radiation block
 * modification toggles, contamination toggles, railgun stats, neutron activation, digamma GUI
 * position, per-hazard disable flags, and pollution/soot-fog/lead-poisoning toggles. Registered
 * into {@link HbmConfig}'s COMMON spec.
 */
public class RadiationConfig {

    public static IntValue FALLOUT_RAIN_DURATION;
    public static IntValue FALLOUT_RAIN_RADIATION;
    public static IntValue FOG_THRESHOLD;
    public static IntValue FOG_CHANCE;
    public static IntValue WORLD_RAD_COUNT;
    public static IntValue WORLD_RAD_THRESHOLD;
    public static BooleanValue WORLD_RAD_EFFECTS;
    public static BooleanValue ENABLE_CONTAMINATION;
    public static BooleanValue ENABLE_CONTAMINATION_ON_GROUND;
    public static IntValue BLOCKS_FALLING_CHANCE;

    public static IntValue RAILGUN_DAMAGE;
    public static IntValue RAILGUN_BUFFER;
    public static IntValue RAILGUN_CONSUMPTION;
    public static IntValue FIRE_DURATION;

    public static BooleanValue ITEM_CONTAMINATION;
    public static IntValue ITEM_CONTAMINATION_THRESHOLD;

    public static IntValue DIGAMMA_X;
    public static IntValue DIGAMMA_Y;
    public static IntValue RAD_TICK_RATE;
    public static DoubleValue RAD_HALF_LIFE_SECONDS;
    public static DoubleValue RAD_DIFFUSIVITY;

    public static IntValue HAZARD_RATE;
    public static BooleanValue DISABLE_ASBESTOS;
    public static BooleanValue DISABLE_BLINDING;
    public static BooleanValue DISABLE_COAL;
    public static BooleanValue DISABLE_EXPLOSIVE;
    public static BooleanValue DISABLE_HYDRO;
    public static BooleanValue DISABLE_HOT;
    public static BooleanValue DISABLE_COLD;
    public static BooleanValue DISABLE_TOXIC;

    public static BooleanValue ENABLE_POLLUTION;
    public static BooleanValue ENABLE_LEAD_FROM_BLOCKS;
    public static BooleanValue ENABLE_LEAD_POISONING;
    public static BooleanValue ENABLE_SOOT_FOG;
    public static BooleanValue ENABLE_POISON;
    public static DoubleValue BUFF_MOB_THRESHOLD;
    public static DoubleValue SOOT_FOG_THRESHOLD;
    public static DoubleValue SOOT_FOG_DIVISOR;
    public static DoubleValue SMOKE_STACK_SOOT_MULT;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("radiation");

        FALLOUT_RAIN_DURATION = builder.comment("Duration of the thunderstorm after fallout, in ticks (only large explosions). [CE: 13.12_falloutRainDuration]")
                .defineInRange("falloutRainDuration", 2000, 0, Integer.MAX_VALUE);
        FALLOUT_RAIN_RADIATION = builder.comment("Radiation in 100th RADs created by fallout rain. [CE: 13.13_falloutRainRadiation]")
                .defineInRange("falloutRainRadiation", 1000, 0, Integer.MAX_VALUE);
        FOG_THRESHOLD = builder.comment("Radiation in RADs required for fog to spawn. [CE: 13.14_fogThreshold]")
                .defineInRange("fogThreshold", 100, 0, Integer.MAX_VALUE);
        FOG_CHANCE = builder.comment("1:n chance of fog spawning every second. [CE: 13.14_fogChance]")
                .defineInRange("fogChance", 50, 1, Integer.MAX_VALUE);
        WORLD_RAD_COUNT = builder.comment("How many block operations radiation can perform per tick. [CE: 13.15_worldRadCount]")
                .defineInRange("worldRadCount", 10, 0, Integer.MAX_VALUE);
        WORLD_RAD_THRESHOLD = builder.comment("The least amount of RADs required for block modification to happen. [CE: 13.16_worldRadThreshold]")
                .defineInRange("worldRadThreshold", 40, 0, Integer.MAX_VALUE);
        WORLD_RAD_EFFECTS = builder.comment("Whether high radiation levels should perform changes in the world. [CE: 13.17_worldRadEffects]")
                .define("worldRadEffects", true);
        ENABLE_CONTAMINATION = builder.comment("Toggles player contamination (and negative effects from radiation poisoning). [CE: 13.18_enableContamination]")
                .define("enableContamination", true);
        ENABLE_CONTAMINATION_ON_GROUND = builder.comment("Toggles contamination for items lying on the ground. [CE: 13.18.1_enableContaminationOnGround]")
                .define("enableContaminationOnGround", false);
        BLOCKS_FALLING_CHANCE = builder.comment("The chance (percent) that a block with low blast resistance will fall down. -1 disables falling. [CE: 13.19_blocksFallingChance]")
                .defineInRange("blocksFallingChance", 100, -1, 100);

        ITEM_CONTAMINATION = builder.comment("Whether high radiation levels should radiate items in inventory. WARNING: extremely laggy and buggy, keep off unless you know what you're doing. [CE: 7.01_itemContamination]")
                .define("itemContamination", false);
        ITEM_CONTAMINATION_THRESHOLD = builder.comment("Minimum received RADs/s threshold at which items get irradiated. [CE: 7.01_itemContaminationThreshold]")
                .defineInRange("itemContaminationThreshold", 15, 0, Integer.MAX_VALUE);

        DIGAMMA_X = builder.comment("X coordinate of the digamma diagnostic GUI (x=0 is on the right). [CE: 7.02_digammaX]")
                .defineInRange("digammaX", 16, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DIGAMMA_Y = builder.comment("Y coordinate of the digamma diagnostic GUI (y=0 is on the bottom). [CE: 7.03_digammaY]")
                .defineInRange("digammaY", 18, Integer.MIN_VALUE, Integer.MAX_VALUE);
        RAD_TICK_RATE = builder.comment("How many ticks between each radiation system update. 1 = once per tick. [CE: 7.99_CE_01_radTickRate]")
                .defineInRange("radTickRate", 1, 1, Integer.MAX_VALUE);
        RAD_HALF_LIFE_SECONDS = builder.comment("The half life of chunk radiation, in seconds. [CE: 7.99_CE_02_radHalfLifeSeconds]")
                .defineInRange("radHalfLifeSeconds", 120D, 0D, Double.MAX_VALUE);
        RAD_DIFFUSIVITY = builder.comment("The diffusivity of chunk radiation. [CE: 7.99_CE_03_radDiffusivity]")
                .defineInRange("radDiffusivity", 10D, 0D, Double.MAX_VALUE);

        builder.pop();

        builder.push("explosion");

        RAILGUN_DAMAGE = builder.comment("How much damage a railgun death blast does per tick. [CE: 6.20_railgunDamage]")
                .defineInRange("railgunDamage", 1000, 0, Integer.MAX_VALUE);
        RAILGUN_BUFFER = builder.comment("How much RF the railgun can store. [CE: 6.21_railgunBuffer]")
                .defineInRange("railgunBuffer", 500000000, 0, Integer.MAX_VALUE);
        RAILGUN_CONSUMPTION = builder.comment("How much RF the railgun requires per shot. [CE: 6.22_railgunConsumption]")
                .defineInRange("railgunConsumption", 250000000, 0, Integer.MAX_VALUE);
        FIRE_DURATION = builder.comment("How long the railgun's fire blast lasts, in ticks. [CE: 6.23_fireDuration]")
                .defineInRange("fireDuration", 15 * 20, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("hazards");

        HAZARD_RATE = builder.comment("Ticks between application of effects for the hazards. [CE: 14.99_CE_04_hazardRate]")
                .defineInRange("hazardRate", 5, 1, Integer.MAX_VALUE);
        DISABLE_ASBESTOS = builder.comment("Setting true makes the Asbestos hazard do nothing. [CE: 14.99_CE_05_disableAsbestos]")
                .define("disableAsbestos", false);
        DISABLE_BLINDING = builder.comment("Setting true makes the Blinding hazard do nothing. [CE: 14.99_CE_06_disableBlinding]")
                .define("disableBlinding", false);
        DISABLE_COAL = builder.comment("Setting true makes the Coal hazard do nothing. [CE: 14.99_CE_07_disableCoal]")
                .define("disableCoal", false);
        DISABLE_EXPLOSIVE = builder.comment("Setting true makes the Explosive hazard do nothing. [CE: 14.99_CE_08_disableExplosive]")
                .define("disableExplosive", false);
        DISABLE_HYDRO = builder.comment("Setting true makes the Hydro hazard do nothing. [CE: 14.99_CE_09_disableHydro]")
                .define("disableHydro", false);
        DISABLE_HOT = builder.comment("Setting true makes the Hot hazard do nothing. [CE: 14.99_CE_10_disableHot]")
                .define("disableHot", false);
        DISABLE_COLD = builder.comment("Setting true makes the Cold hazard do nothing. [CE: 14.99_CE_11_disableCold]")
                .define("disableCold", false);
        DISABLE_TOXIC = builder.comment("Setting true makes the Toxic hazard do nothing. [CE: 14.99_CE_12_disableToxic]")
                .define("disableToxic", false);

        builder.pop();

        builder.push("pollution");

        ENABLE_POLLUTION = builder.comment("If disabled, none of the pollution related things will work. [CE: 16.01_enablePollution]")
                .define("enablePollution", true);
        ENABLE_LEAD_FROM_BLOCKS = builder.comment("Whether breaking blocks in heavy metal polluted areas will poison the player. [CE: 16.02_enableLeadFromBlocks]")
                .define("enableLeadFromBlocks", true);
        ENABLE_LEAD_POISONING = builder.comment("Whether being in a heavy metal polluted area will poison the player. [CE: 16.03_enableLeadPoisoning]")
                .define("enableLeadPoisoning", true);
        ENABLE_SOOT_FOG = builder.comment("Whether smog should be visible. [CE: 16.04_enableSootFog]")
                .define("enableSootFog", true);
        ENABLE_POISON = builder.comment("Whether being in a poisoned area will affect the player. [CE: 16.05_enablePoison]")
                .define("enablePoison", true);
        BUFF_MOB_THRESHOLD = builder.comment("The amount of soot required to buff naturally spawning mobs. [CE: 16.06_buffMobThreshold]")
                .defineInRange("buffMobThreshold", 15D, 0D, Double.MAX_VALUE);
        SOOT_FOG_THRESHOLD = builder.comment("How much soot is required for smog to become visible. [CE: 16.07_sootFogThreshold]")
                .defineInRange("sootFogThreshold", 35D, 0D, Double.MAX_VALUE);
        SOOT_FOG_DIVISOR = builder.comment("The divisor for smog; higher numbers require more soot for the same smog density. [CE: 16.08_sootFogDivisor]")
                .defineInRange("sootFogDivisor", 120D, 1D, Double.MAX_VALUE);
        SMOKE_STACK_SOOT_MULT = builder.comment("How much smokestacks multiply soot by; decimal values reduce soot. [CE: 16.09_smokeStackSootMult]")
                .defineInRange("smokeStackSootMult", 0.8D, 0D, Double.MAX_VALUE);

        builder.pop();
    }

    /**
     * Mirrors CE's Rampant Mode post-load mutation {@code sootFogThreshold *= pollutionMult},
     * applied here as a derived getter reading {@link MobConfig#effectivePollutionMult()} instead
     * of mutating the stored config value from another class.
     */
    public static double sootFogThreshold() {
        if (!MobConfig.RAMPANT_MODE.get()) return SOOT_FOG_THRESHOLD.get();
        return SOOT_FOG_THRESHOLD.get() * MobConfig.effectivePollutionMult();
    }
}
