package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's {@code WeaponConfig}: radar range/buffer/altitude, CIWS accuracy modifier, and the
 * dangerous-drop-effect toggles. Registered into {@link HbmConfig}'s COMMON spec.
 */
public class WeaponConfig {

    public static IntValue RADAR_RANGE;
    public static IntValue RADAR_BUFFER;
    public static IntValue RADAR_ALTITUDE;
    public static IntValue CIWS_ACCURACY;
    public static BooleanValue DROP_MISSILE_PARTS;

    public static BooleanValue DROP_CELL;
    public static BooleanValue DROP_SINGULARITY;
    public static BooleanValue DROP_STAR;
    public static BooleanValue DROP_CRYSTAL;
    public static BooleanValue DROP_DEAD_MANS_EXPLOSIVE;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("missile_machines");

        RADAR_RANGE = builder.comment("Range of the radar; 50 results in a 100x100 block area covered. [CE: 7.00_radarRange]")
                .defineInRange("radarRange", 1000, 0, Integer.MAX_VALUE);
        RADAR_BUFFER = builder.comment("How high entities have to be above the radar to be detected. [CE: 7.01_radarBuffer]")
                .defineInRange("radarBuffer", 30, 0, Integer.MAX_VALUE);
        RADAR_ALTITUDE = builder.comment("Y height required for the radar to work. [CE: 7.02_radarAltitude]")
                .defineInRange("radarAltitude", 55, 0, Integer.MAX_VALUE);
        CIWS_ACCURACY = builder.comment("Additional modifier for CIWS accuracy. [CE: 7.03_ciwsAccuracy]")
                .defineInRange("ciwsAccuracy", 50, 0, Integer.MAX_VALUE);
        DROP_MISSILE_PARTS = builder.comment("Whether shot-down missiles drop items. [CE: 7.03_dropMissileParts]")
                .define("dropMissileParts", true);

        builder.pop();

        builder.push("dangerous_drops");

        DROP_CELL = builder.comment("Whether antimatter cells should explode when dropped. [CE: 10.00_dropCell]")
                .define("dropCell", true);
        DROP_SINGULARITY = builder.comment("Whether singularities and black holes should spawn when dropped. [CE: 10.01_dropBHole]")
                .define("dropBHole", true);
        DROP_STAR = builder.comment("Whether rigged star blaster cells should explode when dropped. [CE: 10.02_dropStar]")
                .define("dropStar", true);
        DROP_CRYSTAL = builder.comment("Whether xen crystals should move blocks when dropped. [CE: 10.04_dropCrys]")
                .define("dropCrys", true);
        DROP_DEAD_MANS_EXPLOSIVE = builder.comment("Whether dead man's explosives should explode when dropped. [CE: 10.05_dropDead]")
                .define("dropDead", true);

        builder.pop();
    }
}
