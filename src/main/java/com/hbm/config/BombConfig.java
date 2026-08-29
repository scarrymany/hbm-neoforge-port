package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's {@code BombConfig}: per-weapon nuke blast radii, custom-nuke radius caps, and the
 * mk3/mk5 explosion engine tunables. Registered into {@link HbmConfig}'s COMMON spec.
 */
public class BombConfig {

    public static IntValue GADGET_RADIUS;
    public static IntValue BOY_RADIUS;
    public static IntValue MAN_RADIUS;
    public static IntValue MIKE_RADIUS;
    public static IntValue TSAR_RADIUS;
    public static IntValue PROTOTYPE_RADIUS;
    public static IntValue FLEIJA_RADIUS;
    public static IntValue MISSILE_RADIUS;
    public static IntValue MIRV_RADIUS;
    public static IntValue FATMAN_RADIUS;
    public static IntValue NUKA_RADIUS;
    public static IntValue ASCHRAB_RADIUS;
    public static IntValue SOLINIUM_RADIUS;
    public static IntValue N2_RADIUS;

    public static IntValue RIGGED_STAR_RANGE;
    public static IntValue RIGGED_STAR_TICKS;

    public static IntValue MAX_CUSTOM_TNT_RADIUS;
    public static IntValue MAX_CUSTOM_NUKE_RADIUS;
    public static IntValue MAX_CUSTOM_HYDRO_RADIUS;
    public static IntValue MAX_CUSTOM_DIRTY_RADIUS;
    public static IntValue MAX_CUSTOM_BALE_RADIUS;
    public static IntValue MAX_CUSTOM_SCHRAB_RADIUS;
    public static IntValue MAX_CUSTOM_SOL_RADIUS;
    public static IntValue MAX_CUSTOM_EUPH_LVL;

    public static IntValue LIMIT_EXPLOSION_LIFESPAN;
    public static IntValue BLAST_SPEED;
    public static IntValue MK5_BLAST_TIME;
    public static IntValue FALLOUT_RANGE;
    public static IntValue FALLOUT_CHUNK_SPEED;
    public static IntValue FALLOUT_DELAY;
    public static BooleanValue DISABLE_NUCLEAR;
    public static BooleanValue ENABLE_NUKE_CLOUDS;
    public static BooleanValue ENABLE_NUKE_NBT_SAVING;
    public static BooleanValue ENABLE_CHUNK_LOADING;
    public static IntValue EXPLOSION_ALGORITHM;
    public static IntValue EXPLOSION_MAX_THREADS;
    public static BooleanValue SAFE_COMMIT;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("nukes");

        GADGET_RADIUS = builder.comment("Radius of the Gadget. [CE: 3.00_gadgetRadius]")
                .defineInRange("gadgetRadius", 150, 0, Integer.MAX_VALUE);
        BOY_RADIUS = builder.comment("Radius of Little Boy. [CE: 3.01_boyRadius]")
                .defineInRange("boyRadius", 120, 0, Integer.MAX_VALUE);
        MAN_RADIUS = builder.comment("Radius of Fat Man. [CE: 3.02_manRadius]")
                .defineInRange("manRadius", 175, 0, Integer.MAX_VALUE);
        MIKE_RADIUS = builder.comment("Radius of Ivy Mike. [CE: 3.03_mikeRadius]")
                .defineInRange("mikeRadius", 250, 0, Integer.MAX_VALUE);
        TSAR_RADIUS = builder.comment("Radius of the Tsar Bomba. [CE: 3.04_tsarRadius]")
                .defineInRange("tsarRadius", 500, 0, Integer.MAX_VALUE);
        PROTOTYPE_RADIUS = builder.comment("Radius of the Prototype. [CE: 3.05_prototypeRadius]")
                .defineInRange("prototypeRadius", 150, 0, Integer.MAX_VALUE);
        FLEIJA_RADIUS = builder.comment("Radius of F.L.E.I.J.A. [CE: 3.06_fleijaRadius]")
                .defineInRange("fleijaRadius", 50, 0, Integer.MAX_VALUE);
        MISSILE_RADIUS = builder.comment("Radius of the nuclear missile. [CE: 3.07_missileRadius]")
                .defineInRange("missileRadius", 100, 0, Integer.MAX_VALUE);
        MIRV_RADIUS = builder.comment("Radius of a MIRV. [CE: 3.08_mirvRadius]")
                .defineInRange("mirvRadius", 70, 0, Integer.MAX_VALUE);
        FATMAN_RADIUS = builder.comment("Radius of the Fatman Launcher. [CE: 3.09_fatmanRadius]")
                .defineInRange("fatmanRadius", 35, 0, Integer.MAX_VALUE);
        NUKA_RADIUS = builder.comment("Radius of the nuka grenade. [CE: 3.10_nukaRadius]")
                .defineInRange("nukaRadius", 25, 0, Integer.MAX_VALUE);
        ASCHRAB_RADIUS = builder.comment("Radius of dropped anti schrabidium. [CE: 3.11_aSchrabRadius]")
                .defineInRange("aSchrabRadius", 20, 0, Integer.MAX_VALUE);
        SOLINIUM_RADIUS = builder.comment("Radius of the blue rinse. [CE: 3.12_soliniumRadius]")
                .defineInRange("soliniumRadius", 150, 0, Integer.MAX_VALUE);
        N2_RADIUS = builder.comment("Radius of the N2 mine. [CE: 3.13_n2Radius]")
                .defineInRange("n2Radius", 200, 0, Integer.MAX_VALUE);

        RIGGED_STAR_RANGE = builder.comment("Radius of the Rigged Star Blaster Energy Cell. [CE: 3.14_riggedStarRadius]")
                .defineInRange("riggedStarRadius", 50, 0, Integer.MAX_VALUE);
        RIGGED_STAR_TICKS = builder.comment("Time in ticks before the Rigged Star Blaster Energy Cell explodes after being dropped - default 60s. [CE: 3.15_riggedStarFuse]")
                .defineInRange("riggedStarFuse", 1200, 0, Integer.MAX_VALUE);

        MAX_CUSTOM_TNT_RADIUS = builder.comment("Maximum TNT radius of custom nukes. [CE: 3.16_maxCustomTNTRadius]")
                .defineInRange("maxCustomTNTRadius", 150, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_NUKE_RADIUS = builder.comment("Maximum Nuke radius of custom nukes. [CE: 3.17_maxCustomNukeRadius]")
                .defineInRange("maxCustomNukeRadius", 200, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_HYDRO_RADIUS = builder.comment("Maximum Thermonuclear radius of custom nukes. [CE: 3.18_maxCustomHydroRadius]")
                .defineInRange("maxCustomHydroRadius", 350, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_DIRTY_RADIUS = builder.comment("Maximum fallout additional radius that can be added to custom nukes. [CE: 3.19_maxCustomDirtyRadius]")
                .defineInRange("maxCustomDirtyRadius", 200, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_BALE_RADIUS = builder.comment("Maximum balefire radius of custom nukes. [CE: 3.20_maxCustomBaleRadius]")
                .defineInRange("maxCustomBaleRadius", 350, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_SCHRAB_RADIUS = builder.comment("Maximum Antischrabidium radius of custom nukes. [CE: 3.21_maxCustomSchrabRadius]")
                .defineInRange("maxCustomSchrabRadius", 250, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_SOL_RADIUS = builder.comment("Maximum Solinium radius of custom nukes. [CE: 3.22_maxCustomSolRadius]")
                .defineInRange("maxCustomSolRadius", 350, 0, Integer.MAX_VALUE);
        MAX_CUSTOM_EUPH_LVL = builder.comment("Maximum Euphemium level of custom nukes (1 level = 100 Rays). [CE: 3.23_maxCustomEuphLvl]")
                .defineInRange("maxCustomEuphLvl", 20, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("explosion");

        LIMIT_EXPLOSION_LIFESPAN = builder.comment("How long an explosion can be unloaded until it dies, in seconds (based on system time). 0 disables the effect. [CE: 6.00_limitExplosionLifespan]")
                .defineInRange("limitExplosionLifespan", 0, 0, Integer.MAX_VALUE);
        BLAST_SPEED = builder.comment("Base speed of MK3 system (old and schrabidium) detonations, in blocks/tick. [CE: 6.01_blastSpeed]")
                .defineInRange("blastSpeed", 1024, 0, Integer.MAX_VALUE);
        MK5_BLAST_TIME = builder.comment("Maximum amount of milliseconds per tick allocated for mk5 chunk processing. [CE: 6.02_mk5BlastTime]")
                .defineInRange("mk5BlastTime", 40, 0, Integer.MAX_VALUE);
        FALLOUT_RANGE = builder.comment("Radius of fallout area (base radius * value in percent). [CE: 6.03_falloutRange]")
                .defineInRange("falloutRange", 100, 0, Integer.MAX_VALUE);
        FALLOUT_CHUNK_SPEED = builder.comment("Process a chunk every nth tick by the fallout rain. [CE: 6.04_falloutChunkSpeed]")
                .defineInRange("falloutChunkSpeed", 5, 1, Integer.MAX_VALUE);
        FALLOUT_DELAY = builder.comment("Maximum amount of milliseconds per tick allocated for fallout chunk processing. [CE: 6.05_falloutTime]")
                .defineInRange("falloutTime", 30, 0, Integer.MAX_VALUE);
        DISABLE_NUCLEAR = builder.comment("Disable the nuclear part of nukes. [CE: 6.07_disableNuclear]")
                .define("disableNuclear", false);
        ENABLE_NUKE_CLOUDS = builder.comment("WARNING: an old config option. Allows nuclear explosions to even happen. [CE: 6.08_enableMushroomClouds]")
                .define("enableMushroomClouds", true);
        ENABLE_NUKE_NBT_SAVING = builder.comment("If true, nukes will save the blocks they want to destroy so they can resume work rather than restart after a crash/reload. For big nukes this can take a while. [CE: 6.09_enableNukeNBTSaving]")
                .define("enableNukeNBTSaving", true);
        ENABLE_CHUNK_LOADING = builder.comment("Allows mk5 explosions to generate new chunks. [CE: 6.10_enableChunkLoading]")
                .define("enableChunkLoading", true);
        EXPLOSION_ALGORITHM = builder.comment("Configures the algorithm used for mk5 explosions. 0 = Legacy, 1 = Threaded DDA, 2 = Threaded DDA with damage accumulation. [CE: 6.11_explosionAlgorithm]")
                .defineInRange("explosionAlgorithm", 2, 0, 2);
        EXPLOSION_MAX_THREADS = builder.comment("Configures the maximum thread count for the threaded DDA explosion algorithm. -N = CPU count - N, 0 = CPU count, N = N. [CE: 6.11.1_explosionMaxThreads]")
                .defineInRange("explosionMaxThreads", -1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        SAFE_COMMIT = builder.comment("Prefer safety over performance (~30% slower). Affects algorithm 1, 2, and the fallout rain effect. [CE: 6.11.2_safeCommit]")
                .define("safeCommit", false);

        builder.pop();
    }
}
