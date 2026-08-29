package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's {@code MobConfig}: Maskman/FBI raid/radiation elemental spawn tuning, duck button,
 * mob gear/weapon toggles, and the Glyphid spawn/swarm/Rampant Mode system. Registered into
 * {@link HbmConfig}'s COMMON spec.
 * <p>
 * CE's {@code int[3]} spawn-chance tuples (e.g. {@code glyphidChance = {50, -40, 0}}) are ported
 * as three separate named {@link IntValue}s each, per field, since {@code ModConfigSpec} has no
 * fixed-size tuple type; {@link #glyphidChance()} etc. below reassemble them into an {@code int[]}
 * for call sites that want the CE-shaped array.
 * <p>
 * CE's Rampant Mode block imperatively mutated {@code RadiationConfig.sootFogThreshold} at
 * config-load time when {@code rampantMode} was on. That's converted to the derived getter
 * {@link RadiationConfig#sootFogThreshold()} multiplying by {@link #pollutionMultEffective()}
 * on read, instead of mutating another class's stored config value.
 */
public class MobConfig {

    public static BooleanValue ENABLE_MASKMAN;
    public static IntValue MASKMAN_DELAY;
    public static IntValue MASKMAN_CHANCE;
    public static IntValue MASKMAN_MIN_RAD;
    public static BooleanValue MASKMAN_UNDERGROUND;

    public static BooleanValue ENABLE_FBI_RAIDS;
    public static IntValue RAID_DELAY;
    public static IntValue RAID_CHANCE;
    public static IntValue RAID_AMOUNT;
    public static IntValue RAID_ATTACK_DELAY;
    public static IntValue RAID_ATTACK_REACH;
    public static IntValue RAID_ATTACK_DISTANCE;
    public static IntValue RAID_DRONES;

    public static BooleanValue ENABLE_MELTDOWN_ELEMENTALS;
    public static IntValue ELEMENTAL_DELAY;
    public static IntValue ELEMENTAL_CHANCE;
    public static IntValue ELEMENTAL_AMOUNT;
    public static IntValue ELEMENTAL_ATTACK_DISTANCE;

    public static BooleanValue ENABLE_DUCKS;
    public static BooleanValue ENABLE_MOB_GEAR;
    public static BooleanValue ENABLE_MOB_WEAPONS;
    public static DoubleValue MOB_WEAPON_SOOT_REDUCTION;

    public static BooleanValue ENABLE_HIVES;
    public static IntValue HIVE_SPAWN;
    public static DoubleValue SCOUT_THRESHOLD;
    public static DoubleValue SPAWN_MAX;
    public static DoubleValue TARGETING_THRESHOLD;
    public static IntValue SCOUT_SWARM_SPAWN_CHANCE;
    public static IntValue LARGE_HIVE_CHANCE;
    public static IntValue LARGE_HIVE_THRESHOLD;
    public static BooleanValue WAYPOINT_DEBUG;

    public static BooleanValue ENABLE_INFESTATION;
    public static DoubleValue BASE_INFEST_CHANCE;

    public static IntValue BASE_SWARM_SIZE;
    public static DoubleValue SWARM_SCALING_MULT;
    public static IntValue SOOT_STEP;
    public static IntValue SWARM_COOLDOWN_SECONDS;

    public static IntValue GLYPHID_CHANCE_BASE, GLYPHID_CHANCE_MODIFIER, GLYPHID_CHANCE_MIN_SOOT;
    public static IntValue BRAWLER_CHANCE_BASE, BRAWLER_CHANCE_MODIFIER, BRAWLER_CHANCE_MIN_SOOT;
    public static IntValue BOMBARDIER_CHANCE_BASE, BOMBARDIER_CHANCE_MODIFIER, BOMBARDIER_CHANCE_MIN_SOOT;
    public static IntValue BLASTER_CHANCE_BASE, BLASTER_CHANCE_MODIFIER, BLASTER_CHANCE_MIN_SOOT;
    public static IntValue DIGGER_CHANCE_BASE, DIGGER_CHANCE_MODIFIER, DIGGER_CHANCE_MIN_SOOT;
    public static IntValue BEHEMOTH_CHANCE_BASE, BEHEMOTH_CHANCE_MODIFIER, BEHEMOTH_CHANCE_MIN_SOOT;
    public static IntValue BRENDA_CHANCE_BASE, BRENDA_CHANCE_MODIFIER, BRENDA_CHANCE_MIN_SOOT;
    public static IntValue JOHNSON_CHANCE_BASE, JOHNSON_CHANCE_MODIFIER, JOHNSON_CHANCE_MIN_SOOT;

    public static BooleanValue RAMPANT_MODE;
    public static BooleanValue RAMPANT_NATURAL_SCOUT_SPAWN;
    public static DoubleValue RAMPANT_SCOUT_SPAWN_THRESH;
    public static IntValue RAMPANT_SCOUT_SPAWN_CHANCE;
    public static BooleanValue RAMPANT_EXTENDED_TARGETING;
    public static BooleanValue RAMPANT_DIG;
    public static BooleanValue RAMPANT_GLYPHID_GUIDANCE;
    public static DoubleValue RAMPANT_SMOKE_STACK_OVERRIDE;
    public static BooleanValue SCOUT_INITIAL_SPAWN;
    public static DoubleValue POLLUTION_MULT;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("mobs");

        ENABLE_MASKMAN = builder.comment("Whether mask man should spawn. [CE: 12.M00_enableMaskman]").define("enableMaskman", true);
        MASKMAN_DELAY = builder.comment("How many world ticks need to pass for a check to be performed. [CE: 12.M01_maskmanDelay]").defineInRange("maskmanDelay", 60 * 60 * 60, 1, Integer.MAX_VALUE);
        MASKMAN_CHANCE = builder.comment("1:x chance to spawn mask man, must be at least 1. [CE: 12.M02_maskmanChance]").defineInRange("maskmanChance", 3, 1, Integer.MAX_VALUE);
        MASKMAN_MIN_RAD = builder.comment("The amount of radiation needed for mask man to spawn. [CE: 12.M03_maskmanMinRad]").defineInRange("maskmanMinRad", 50, 0, Integer.MAX_VALUE);
        MASKMAN_UNDERGROUND = builder.comment("Whether players need to be underground for mask man to spawn. [CE: 12.M04_maskmanUnderound]").define("maskmanUnderground", true);

        ENABLE_FBI_RAIDS = builder.comment("Whether there should be FBI raids. [CE: 12.F00_enableFBIRaids]").define("enableFBIRaids", false);
        RAID_DELAY = builder.comment("How many world ticks need to pass for a check to be performed. [CE: 12.F01_raidDelay]").defineInRange("raidDelay", 30 * 60 * 60, 1, Integer.MAX_VALUE);
        RAID_CHANCE = builder.comment("1:x chance to spawn a raid, must be at least 1. [CE: 12.F02_raidChance]").defineInRange("raidChance", 3, 1, Integer.MAX_VALUE);
        RAID_AMOUNT = builder.comment("How many FBI agents are spawned each raid. [CE: 12.F03_raidAmount]").defineInRange("raidAmount", 15, 0, Integer.MAX_VALUE);
        RAID_ATTACK_DELAY = builder.comment("Time between individual attempts to break machines. [CE: 12.F04_raidAttackDelay]").defineInRange("raidAttackDelay", 40, 0, Integer.MAX_VALUE);
        RAID_ATTACK_REACH = builder.comment("How far away machines can be broken. [CE: 12.F05_raidAttackReach]").defineInRange("raidAttackReach", 2, 0, Integer.MAX_VALUE);
        RAID_ATTACK_DISTANCE = builder.comment("How far away agents will spawn from the targeted player. [CE: 12.F06_raidAttackDistance]").defineInRange("raidAttackDistance", 32, 0, Integer.MAX_VALUE);
        RAID_DRONES = builder.comment("How many quadcopter drones are spawned each raid. [CE: 12.F07_raidDrones]").defineInRange("raidDrones", 5, 0, Integer.MAX_VALUE);

        ENABLE_MELTDOWN_ELEMENTALS = builder.comment("Whether there should be radiation elementals. [CE: 12.E00_enableMeltdownElementals]").define("enableMeltdownElementals", true);
        ELEMENTAL_DELAY = builder.comment("How many world ticks need to pass for a check to be performed. [CE: 12.E01_elementalDelay]").defineInRange("elementalDelay", 30 * 60 * 60, 1, Integer.MAX_VALUE);
        ELEMENTAL_CHANCE = builder.comment("1:x chance to spawn elementals, must be at least 1. [CE: 12.E02_elementalChance]").defineInRange("elementalChance", 2, 1, Integer.MAX_VALUE);
        ELEMENTAL_AMOUNT = builder.comment("How many elementals are spawned each raid. [CE: 12.E03_elementalAmount]").defineInRange("elementalAmount", 10, 0, Integer.MAX_VALUE);
        ELEMENTAL_ATTACK_DISTANCE = builder.comment("How far away elementals will spawn from the targeted player. [CE: 12.E04_elementalAttackDistance]").defineInRange("elementalAttackDistance", 32, 0, Integer.MAX_VALUE);

        ENABLE_DUCKS = builder.comment("Whether pressing O should allow the player to duck. [CE: 12.D00_enableDucks]").define("enableDucks", true);
        ENABLE_MOB_GEAR = builder.comment("Whether zombies and skeletons should have additional gear when spawning. [CE: 12.D01_enableMobGear]").define("enableMobGear", true);
        ENABLE_MOB_WEAPONS = builder.comment("Whether skeletons should have bows replaced with guns when spawning at higher soot levels. [CE: 12.D02_enableMobWeapons]").define("enableMobWeapons", true);
        MOB_WEAPON_SOOT_REDUCTION = builder.comment("Reduces the amount of soot needed for skeleton guns to appear. [CE: 12.D03_mobWeaponSootReduction]").defineInRange("mobWeaponSootReduction", 0D, 0D, Double.MAX_VALUE);

        ENABLE_HIVES = builder.comment("Whether glyphid hives should spawn. [CE: 12.G00_enableHives]").define("enableHives", true);
        HIVE_SPAWN = builder.comment("The average amount of chunks per hive. [CE: 12.G01_hiveSpawn]").defineInRange("hiveSpawn", 256, 1, Integer.MAX_VALUE);
        SCOUT_THRESHOLD = builder.comment("Minimum amount of soot for scouts to spawn. [CE: 12.G02_scoutThreshold]").defineInRange("scoutThreshold", 1D, 0D, Double.MAX_VALUE);
        SPAWN_MAX = builder.comment("Maximum amount of glyphids able to exist at once through natural spawning. [CE: 12.G07_spawnMax]").defineInRange("spawnMax", 50D, 0D, Double.MAX_VALUE);
        TARGETING_THRESHOLD = builder.comment("Minimum amount of soot required for glyphids' extended targeting range to activate. [CE: 12.G08_targetingThreshold]").defineInRange("targetingThreshold", 1D, 0D, Double.MAX_VALUE);
        SCOUT_SWARM_SPAWN_CHANCE = builder.comment("How likely scouts are to spawn in swarms, 1 in x chance. [CE: 12.G10_scoutSwarmSpawn]").defineInRange("scoutSwarmSpawnChance", 3, 1, Integer.MAX_VALUE);
        LARGE_HIVE_CHANCE = builder.comment("The chance for a large hive to spawn, formula 1/x. [CE: 12.G11_largeHiveChance]").defineInRange("largeHiveChance", 5, 1, Integer.MAX_VALUE);
        LARGE_HIVE_THRESHOLD = builder.comment("The soot threshold for a large hive to spawn. [CE: 12.G12_largeHiveThreshold]").defineInRange("largeHiveThreshold", 20, 0, Integer.MAX_VALUE);
        WAYPOINT_DEBUG = builder.comment("Allows glyphid waypoints to be seen, mainly for debugging, also useful as an aid against them. [CE: 12.G13_waypointDebug]").define("waypointDebug", false);

        ENABLE_INFESTATION = builder.comment("Whether structures infested with glyphids should spawn. [CE: 12.I01_enableInfestation]").define("enableInfestation", true);
        BASE_INFEST_CHANCE = builder.comment("The chance for infested structures to spawn. [CE: 12.I02_baseInfestChance]").defineInRange("baseInfestChance", 5D, 0D, 100D);

        builder.comment(
                "General Glyphid spawn logic configuration",
                "",
                "The first number is the base chance which applies at 0 soot,",
                "the second number is the modifier that applies with soot based on the formula below,",
                "the third number is a hard minimum of soot for this type to spawn.",
                "Negative base chances mean that glyphids won't spawn outright, negative modifiers mean that the type becomes less likely with higher soot.",
                "The formula for glyphid spawning chance is: (base chance + (modifier - modifier / max((soot + 1)/3, 3)))",
                "The formula for glyphid swarm scaling is: (baseSwarmSize * max(swarmScalingMult * soot/sootStep, 1))"
        ).push("glyphid_chances");

        BASE_SWARM_SIZE = builder.comment("The basic, soot-less swarm size. [CE: 12.GS01_baseSwarmSize]").defineInRange("baseSwarmSize", 5, 0, Integer.MAX_VALUE);
        SWARM_SCALING_MULT = builder.comment("By how much swarm size should scale per soot amount determined below. [CE: 12.GS02_swarmScalingMult]").defineInRange("swarmScalingMult", 1.2D, 0D, Double.MAX_VALUE);
        SOOT_STEP = builder.comment("The soot amount the above multiplier applies to the swarm size. [CE: 12.GS03_sootStep]").defineInRange("sootStep", 50, 1, Integer.MAX_VALUE);
        SWARM_COOLDOWN_SECONDS = builder.comment("How often glyphid swarms spawn, in seconds. [CE: 12.GS04_swarmCooldown]").defineInRange("swarmCooldownSeconds", 120, 0, Integer.MAX_VALUE);

        GLYPHID_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid grunt. [CE: 12.GC01_glyphidChance[0]]").defineInRange("glyphidChanceBase", 50, Integer.MIN_VALUE, Integer.MAX_VALUE);
        GLYPHID_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid grunt. [CE: 12.GC01_glyphidChance[1]]").defineInRange("glyphidChanceModifier", -45, Integer.MIN_VALUE, Integer.MAX_VALUE);
        GLYPHID_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid grunt to spawn. [CE: 12.GC01_glyphidChance[2]]").defineInRange("glyphidChanceMinSoot", 0, 0, Integer.MAX_VALUE);

        BRAWLER_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid brawler. [CE: 12.GC02_brawlerChance[0]]").defineInRange("brawlerChanceBase", 10, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BRAWLER_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid brawler. [CE: 12.GC02_brawlerChance[1]]").defineInRange("brawlerChanceModifier", 30, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BRAWLER_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid brawler to spawn. [CE: 12.GC02_brawlerChance[2]]").defineInRange("brawlerChanceMinSoot", 1, 0, Integer.MAX_VALUE);

        BOMBARDIER_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid bombardier. [CE: 12.GC03_bombardierChance[0]]").defineInRange("bombardierChanceBase", 20, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BOMBARDIER_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid bombardier. [CE: 12.GC03_bombardierChance[1]]").defineInRange("bombardierChanceModifier", -15, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BOMBARDIER_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid bombardier to spawn. [CE: 12.GC03_bombardierChance[2]]").defineInRange("bombardierChanceMinSoot", 1, 0, Integer.MAX_VALUE);

        BLASTER_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid blaster. [CE: 12.GC04_blasterChance[0]]").defineInRange("blasterChanceBase", -5, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BLASTER_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid blaster. [CE: 12.GC04_blasterChance[1]]").defineInRange("blasterChanceModifier", 40, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BLASTER_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid blaster to spawn. [CE: 12.GC04_blasterChance[2]]").defineInRange("blasterChanceMinSoot", 5, 0, Integer.MAX_VALUE);

        DIGGER_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid digger. [CE: 12.GC05_diggerChance[0]]").defineInRange("diggerChanceBase", -15, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DIGGER_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid digger. [CE: 12.GC05_diggerChance[1]]").defineInRange("diggerChanceModifier", 25, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DIGGER_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid digger to spawn. [CE: 12.GC05_diggerChance[2]]").defineInRange("diggerChanceMinSoot", 5, 0, Integer.MAX_VALUE);

        BEHEMOTH_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid behemoth. [CE: 12.GC06_behemothChance[0]]").defineInRange("behemothChanceBase", -30, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BEHEMOTH_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid behemoth. [CE: 12.GC06_behemothChance[1]]").defineInRange("behemothChanceModifier", 45, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BEHEMOTH_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid behemoth to spawn. [CE: 12.GC06_behemothChance[2]]").defineInRange("behemothChanceMinSoot", 10, 0, Integer.MAX_VALUE);

        BRENDA_CHANCE_BASE = builder.comment("Base spawn chance for a glyphid brenda. [CE: 12.GC07_brendaChance[0]]").defineInRange("brendaChanceBase", -50, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BRENDA_CHANCE_MODIFIER = builder.comment("Soot modifier for a glyphid brenda. [CE: 12.GC07_brendaChance[1]]").defineInRange("brendaChanceModifier", 60, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BRENDA_CHANCE_MIN_SOOT = builder.comment("Minimum soot for a glyphid brenda to spawn. [CE: 12.GC07_brendaChance[2]]").defineInRange("brendaChanceMinSoot", 20, 0, Integer.MAX_VALUE);

        JOHNSON_CHANCE_BASE = builder.comment("Base spawn chance for Big Man Johnson. [CE: 12.GC08_johnsonChance[0]]").defineInRange("johnsonChanceBase", -50, Integer.MIN_VALUE, Integer.MAX_VALUE);
        JOHNSON_CHANCE_MODIFIER = builder.comment("Soot modifier for Big Man Johnson. [CE: 12.GC08_johnsonChance[1]]").defineInRange("johnsonChanceModifier", 60, Integer.MIN_VALUE, Integer.MAX_VALUE);
        JOHNSON_CHANCE_MIN_SOOT = builder.comment("Minimum soot for Big Man Johnson to spawn. [CE: 12.GC08_johnsonChance[2]]").defineInRange("johnsonChanceMinSoot", 50, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.comment(
                "Rampant Mode changes glyphid behavior and spawning to be more aggressive, changes include:",
                "",
                "Glyphid Scouts will naturally spawn alongside normal mobs if soot levels are above a certain threshold",
                "Glyphids will always have the extended targeting enabled",
                "Glyphids can dig to waypoints",
                "The Glyphids will expand always toward your base",
                "Scouts will spawn from the start, making glyphids start expanding off the bat",
                "Smokestacks have reduced efficiency, only reducing soot by 40%"
        ).push("rampant");

        RAMPANT_MODE = builder.comment("The main rampant mode toggle, enables all other features associated with it. The individual features below can also be used regardless of whether this master toggle is on. [CE: 12.R01_rampantMode]").define("rampantMode", false);

        RAMPANT_NATURAL_SCOUT_SPAWN = builder.comment("Whether scouts should spawn naturally in highly polluted chunks. [CE: 12.R02_rampantScoutSpawn]").define("rampantScoutSpawn", false);
        RAMPANT_SCOUT_SPAWN_THRESH = builder.comment("How much soot is needed for scouts to naturally spawn. [CE: 12.R02.1_rampantScoutSpawnThresh]").defineInRange("rampantScoutSpawnThresh", 13D, 0D, Double.MAX_VALUE);
        RAMPANT_SCOUT_SPAWN_CHANCE = builder.comment("How often scouts naturally spawn per mob population, 1/x format; the bigger the number, the rarer the scouts. [CE: 12.R02.2_rampantScoutSpawnChance]").defineInRange("rampantScoutSpawnChance", 1400, 1, Integer.MAX_VALUE);
        RAMPANT_EXTENDED_TARGETING = builder.comment("Whether Glyphids should have the extended targeting always enabled. [CE: 12.R03_rampantExtendedTargeting]").define("rampantExtendedTargeting", false);
        RAMPANT_DIG = builder.comment("Whether Glyphids should be able to dig to waypoints. [CE: 12.R04_rampantDig]").define("rampantDig", false);
        RAMPANT_GLYPHID_GUIDANCE = builder.comment("Whether Glyphids should always expand toward a player's spawnpoint. [CE: 12.R05_rampantGlyphidGuidance]").define("rampantGlyphidGuidance", false);
        RAMPANT_SMOKE_STACK_OVERRIDE = builder.comment("How much the smokestack should multiply soot by on rampant mode. [CE: 12.R06_rampantSmokeStackOverride]").defineInRange("rampantSmokeStackOverride", 0.4D, 0D, 1D);
        SCOUT_INITIAL_SPAWN = builder.comment("Whether glyphid scouts should be able to spawn on the first swarm of a hive, causing glyphids to expand significantly faster. [CE: 12.R07_scoutInitialSpawn]").define("scoutInitialSpawn", false);
        POLLUTION_MULT = builder.comment("A multiplier for soot emitted, whether you want to increase or decrease it. [CE: 12.R08_pollutionMult]").defineInRange("pollutionMult", 1D, 0D, Double.MAX_VALUE);

        builder.pop(2); // closes "rampant" and "mobs"
    }

    /** Mirrors CE's {@code MobConfig.trueRam()}. */
    public static boolean trueRampantMode() {
        return RAMPANT_MODE.get()
                && effectiveRampantNaturalScoutSpawn()
                && effectiveScoutThreshold() <= 0.1D
                && effectiveRampantExtendedTargeting()
                && effectiveRampantDig()
                && effectiveRampantGlyphidGuidance();
    }

    /**
     * Mirrors CE's post-load Rampant Mode overrides ({@code rampantNaturalScoutSpawn = true},
     * {@code rampantExtendedTargetting = true}, {@code rampantDig = true},
     * {@code rampantGlyphidGuidance = true}, {@code scoutSwarmSpawnChance = 1},
     * {@code scoutThreshold = 0.1}), as derived getters rather than mutated stored values.
     */
    public static boolean effectiveRampantNaturalScoutSpawn() {
        return RAMPANT_MODE.get() || RAMPANT_NATURAL_SCOUT_SPAWN.get();
    }

    public static boolean effectiveRampantExtendedTargeting() {
        return RAMPANT_MODE.get() || RAMPANT_EXTENDED_TARGETING.get();
    }

    public static boolean effectiveRampantDig() {
        return RAMPANT_MODE.get() || RAMPANT_DIG.get();
    }

    public static boolean effectiveRampantGlyphidGuidance() {
        return RAMPANT_MODE.get() || RAMPANT_GLYPHID_GUIDANCE.get();
    }

    public static int effectiveScoutSwarmSpawnChance() {
        return RAMPANT_MODE.get() ? 1 : SCOUT_SWARM_SPAWN_CHANCE.get();
    }

    public static double effectiveScoutThreshold() {
        return RAMPANT_MODE.get() ? 0.1D : SCOUT_THRESHOLD.get();
    }

    public static double effectivePollutionMult() {
        return RAMPANT_MODE.get() && POLLUTION_MULT.get() == 1D ? 3D : POLLUTION_MULT.get();
    }

    public static int effectiveBombardierChanceMinSoot() {
        return RAMPANT_MODE.get() && BOMBARDIER_CHANCE_MIN_SOOT.get() == 1 ? 0 : BOMBARDIER_CHANCE_MIN_SOOT.get();
    }

    public static int[] glyphidChance() { return new int[]{ GLYPHID_CHANCE_BASE.get(), GLYPHID_CHANCE_MODIFIER.get(), GLYPHID_CHANCE_MIN_SOOT.get() }; }
    public static int[] brawlerChance() { return new int[]{ BRAWLER_CHANCE_BASE.get(), BRAWLER_CHANCE_MODIFIER.get(), BRAWLER_CHANCE_MIN_SOOT.get() }; }
    public static int[] bombardierChance() { return new int[]{ BOMBARDIER_CHANCE_BASE.get(), BOMBARDIER_CHANCE_MODIFIER.get(), effectiveBombardierChanceMinSoot() }; }
    public static int[] blasterChance() { return new int[]{ BLASTER_CHANCE_BASE.get(), BLASTER_CHANCE_MODIFIER.get(), BLASTER_CHANCE_MIN_SOOT.get() }; }
    public static int[] diggerChance() { return new int[]{ DIGGER_CHANCE_BASE.get(), DIGGER_CHANCE_MODIFIER.get(), DIGGER_CHANCE_MIN_SOOT.get() }; }
    public static int[] behemothChance() { return new int[]{ BEHEMOTH_CHANCE_BASE.get(), BEHEMOTH_CHANCE_MODIFIER.get(), BEHEMOTH_CHANCE_MIN_SOOT.get() }; }
    public static int[] brendaChance() { return new int[]{ BRENDA_CHANCE_BASE.get(), BRENDA_CHANCE_MODIFIER.get(), BRENDA_CHANCE_MIN_SOOT.get() }; }
    public static int[] johnsonChance() { return new int[]{ JOHNSON_CHANCE_BASE.get(), JOHNSON_CHANCE_MODIFIER.get(), JOHNSON_CHANCE_MIN_SOOT.get() }; }
}
