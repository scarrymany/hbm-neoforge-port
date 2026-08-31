package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Port of CE's {@code StructureConfig}: the master structure-generation toggle, chunk spacing,
 * loot amount factor, and per-structure spawn weights. Registered into {@link HbmConfig}'s
 * COMMON spec.
 * <p>
 * CE's {@code enableStructures} tri-state ("true"/"false"/"flag", where "flag" means "respect the
 * world's generate-structures flag") is ported as a validated enumerated string rather than a raw
 * free-text {@code String} parsed by hand, since {@code defineInList} enforces the valid set
 * directly. {@link #enableStructuresFlag()} returns CE's original int encoding (1/0/2) for
 * mechanical compatibility with call sites ported from CE.
 */
public class StructureConfig {

    public enum StructureFlag { TRUE, FALSE, WORLD_FLAG }

    public static ConfigValue<String> ENABLE_STRUCTURES;
    public static IntValue STRUCTURE_MIN_CHUNKS;
    public static IntValue STRUCTURE_MAX_CHUNKS;
    public static DoubleValue LOOT_AMOUNT_FACTOR;
    public static BooleanValue DEBUG_STRUCTURES;
    public static BooleanValue ENABLE_RUINS;
    public static BooleanValue ENABLE_OCEAN_STRUCTURES;

    public static IntValue SPIRE_SPAWN_WEIGHT;
    public static IntValue FEATURES_SPAWN_WEIGHT;
    public static IntValue BUNKER_SPAWN_WEIGHT;
    public static IntValue VERTIBIRD_SPAWN_WEIGHT;
    public static IntValue VERTIBIRD_CRASHED_SPAWN_WEIGHT;
    public static IntValue AIRCRAFT_CARRIER_SPAWN_WEIGHT;
    public static IntValue OIL_RIG_SPAWN_WEIGHT;
    public static IntValue LIGHTHOUSE_SPAWN_WEIGHT;
    public static IntValue BEACHED_PATROL_SPAWN_WEIGHT;
    public static IntValue DISH_SPAWN_WEIGHT;
    public static IntValue FOREST_CHEM_SPAWN_WEIGHT;
    public static IntValue PLANE_1_SPAWN_WEIGHT;
    public static IntValue PLANE_2_SPAWN_WEIGHT;
    public static IntValue DESERT_SHACK_1_SPAWN_WEIGHT;
    public static IntValue DESERT_SHACK_2_SPAWN_WEIGHT;
    public static IntValue DESERT_SHACK_3_SPAWN_WEIGHT;
    public static IntValue LABORATORY_SPAWN_WEIGHT;
    public static IntValue FOREST_POST_SPAWN_WEIGHT;
    public static IntValue RUIN_A_SPAWN_WEIGHT;
    public static IntValue RUIN_B_SPAWN_WEIGHT;
    public static IntValue RUIN_C_SPAWN_WEIGHT;
    public static IntValue RUIN_D_SPAWN_WEIGHT;
    public static IntValue RUIN_E_SPAWN_WEIGHT;
    public static IntValue RUIN_F_SPAWN_WEIGHT;
    public static IntValue RUIN_G_SPAWN_WEIGHT;
    public static IntValue RUIN_H_SPAWN_WEIGHT;
    public static IntValue RUIN_I_SPAWN_WEIGHT;
    public static IntValue RUIN_J_SPAWN_WEIGHT;
    public static IntValue RADIO_SPAWN_WEIGHT;
    public static IntValue FACTORY_SPAWN_WEIGHT;
    public static IntValue PLAINS_NULL_WEIGHT;
    public static IntValue OCEAN_NULL_WEIGHT;
    public static IntValue CRANE_SPAWN_WEIGHT;
    public static IntValue BROADCASTING_TOWER_SPAWN_WEIGHT;
    public static IntValue METEOR_DUNGEON_SPAWN_WEIGHT;

    public static BooleanValue ENABLE_DYNAMIC_STRUCTURE_SAVING;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("structures");

        // ArrayList, not List.of: NeoForge defineInList uses acceptable::contains and
        // ValueSpec.test(null) on missing keys. Java 21 List.of().contains(null) NPEs.
        ENABLE_STRUCTURES = builder
                .comment("Whether modern NTM structures will spawn. WORLD_FLAG respects the world's \"Generate Structures\" flag. [CE: 15.00_enableStructures]")
                .defineInList("enableStructures", StructureFlag.WORLD_FLAG.name(), new ArrayList<>(List.of(
                        StructureFlag.TRUE.name(), StructureFlag.FALSE.name(), StructureFlag.WORLD_FLAG.name()
                )));
        STRUCTURE_MIN_CHUNKS = builder.comment("Minimum non-zero distance between structures in chunks (settings lower than 8 may be problematic). [CE: 15.01_structureMinChunks]")
                .defineInRange("structureMinChunks", 4, 1, Integer.MAX_VALUE);
        STRUCTURE_MAX_CHUNKS = builder.comment("Maximum non-zero distance between structures in chunks. [CE: 15.02_structureMaxChunks]")
                .defineInRange("structureMaxChunks", 16, 1, Integer.MAX_VALUE);
        LOOT_AMOUNT_FACTOR = builder.comment("General factor for loot spawns. Applies to spawned inventories, not loot blocks. [CE: 15.03_lootAmountFactor]")
                .defineInRange("lootAmountFactor", 1D, 0D, Double.MAX_VALUE);
        DEBUG_STRUCTURES = builder.comment("If enabled, special structure blocks like jigsaw blocks will not be transformed after generating. [CE: 15.04_debugStructures]")
                .define("debugStructures", false);
        ENABLE_RUINS = builder.comment("Toggle for all ruin structures (A through J). [CE: 15.05_enableRuins]")
                .define("enableRuins", true);
        ENABLE_OCEAN_STRUCTURES = builder.comment("Toggle for ocean structures (aircraft carrier, oil rig, lighthouse). [CE: 15.06_enableOceanStructures]")
                .define("enableOceanStructures", true);

        SPIRE_SPAWN_WEIGHT = builder.comment("Spawn weight for spire structure. [CE: 15.07_spireSpawnWeight]").defineInRange("spireSpawnWeight", 2, 0, Integer.MAX_VALUE);
        FEATURES_SPAWN_WEIGHT = builder.comment("Spawn weight for misc structures (houses, offices, etc). [CE: 15.08_featuresSpawnWeight]").defineInRange("featuresSpawnWeight", 50, 0, Integer.MAX_VALUE);
        BUNKER_SPAWN_WEIGHT = builder.comment("Spawn weight for bunker structure. [CE: 15.09_bunkerSpawnWeight]").defineInRange("bunkerSpawnWeight", 6, 0, Integer.MAX_VALUE);
        VERTIBIRD_SPAWN_WEIGHT = builder.comment("Spawn weight for vertibird structure. [CE: 15.10_vertibirdSpawnWeight]").defineInRange("vertibirdSpawnWeight", 6, 0, Integer.MAX_VALUE);
        VERTIBIRD_CRASHED_SPAWN_WEIGHT = builder.comment("Spawn weight for crashed vertibird structure. [CE: 15.11_crashedVertibirdSpawnWeight]").defineInRange("crashedVertibirdSpawnWeight", 10, 0, Integer.MAX_VALUE);
        AIRCRAFT_CARRIER_SPAWN_WEIGHT = builder.comment("Spawn weight for aircraft carrier structure. [CE: 15.12_aircraftCarrierSpawnWeight]").defineInRange("aircraftCarrierSpawnWeight", 3, 0, Integer.MAX_VALUE);
        OIL_RIG_SPAWN_WEIGHT = builder.comment("Spawn weight for oil rig structure. [CE: 15.13_oilRigSpawnWeight]").defineInRange("oilRigSpawnWeight", 5, 0, Integer.MAX_VALUE);
        LIGHTHOUSE_SPAWN_WEIGHT = builder.comment("Spawn weight for lighthouse structure. [CE: 15.14_lighthouseSpawnWeight]").defineInRange("lighthouseSpawnWeight", 1, 0, Integer.MAX_VALUE);
        BEACHED_PATROL_SPAWN_WEIGHT = builder.comment("Spawn weight for beached patrol structure. [CE: 15.15_beachedPatrolSpawnWeight]").defineInRange("beachedPatrolSpawnWeight", 15, 0, Integer.MAX_VALUE);
        DISH_SPAWN_WEIGHT = builder.comment("Spawn weight for dish structures. [CE: 15.16_dishSpawnWeight]").defineInRange("dishSpawnWeight", 10, 0, Integer.MAX_VALUE);
        FOREST_CHEM_SPAWN_WEIGHT = builder.comment("Spawn weight for forest chemical plant structure. [CE: 15.17_forestChemSpawnWeight]").defineInRange("forestChemSpawnWeight", 30, 0, Integer.MAX_VALUE);
        PLANE_1_SPAWN_WEIGHT = builder.comment("Spawn weight for crashed plane 1 structure. [CE: 15.18_plane1SpawnWeight]").defineInRange("plane1SpawnWeight", 25, 0, Integer.MAX_VALUE);
        PLANE_2_SPAWN_WEIGHT = builder.comment("Spawn weight for crashed plane 2 structure. [CE: 15.19_plane2SpawnWeight]").defineInRange("plane2SpawnWeight", 25, 0, Integer.MAX_VALUE);
        DESERT_SHACK_1_SPAWN_WEIGHT = builder.comment("Spawn weight for desert shack 1 structure. [CE: 15.20_desertShack1SpawnWeight]").defineInRange("desertShack1SpawnWeight", 18, 0, Integer.MAX_VALUE);
        DESERT_SHACK_2_SPAWN_WEIGHT = builder.comment("Spawn weight for desert shack 2 structure. [CE: 15.21_desertShack2SpawnWeight]").defineInRange("desertShack2SpawnWeight", 20, 0, Integer.MAX_VALUE);
        DESERT_SHACK_3_SPAWN_WEIGHT = builder.comment("Spawn weight for desert shack 3 structure. [CE: 15.22_desertShack3SpawnWeight]").defineInRange("desertShack3SpawnWeight", 22, 0, Integer.MAX_VALUE);
        LABORATORY_SPAWN_WEIGHT = builder.comment("Spawn weight for laboratory structure. [CE: 15.23_laboratorySpawnWeight]").defineInRange("laboratorySpawnWeight", 20, 0, Integer.MAX_VALUE);
        FOREST_POST_SPAWN_WEIGHT = builder.comment("Spawn weight for forest post structure. [CE: 15.24_forestPostSpawnWeight]").defineInRange("forestPostSpawnWeight", 30, 0, Integer.MAX_VALUE);
        RUIN_A_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin A structure. [CE: 15.25_ruinASpawnWeight]").defineInRange("ruinASpawnWeight", 10, 0, Integer.MAX_VALUE);
        RUIN_B_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin B structure. [CE: 15.26_ruinBSpawnWeight]").defineInRange("ruinBSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_C_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin C structure. [CE: 15.27_ruinCSpawnWeight]").defineInRange("ruinCSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_D_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin D structure. [CE: 15.28_ruinDSpawnWeight]").defineInRange("ruinDSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_E_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin E structure. [CE: 15.29_ruinESpawnWeight]").defineInRange("ruinESpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_F_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin F structure. [CE: 15.30_ruinFSpawnWeight]").defineInRange("ruinFSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_G_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin G structure. [CE: 15.31_ruinGSpawnWeight]").defineInRange("ruinGSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_H_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin H structure. [CE: 15.32_ruinHSpawnWeight]").defineInRange("ruinHSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_I_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin I structure. [CE: 15.33_ruinISpawnWeight]").defineInRange("ruinISpawnWeight", 12, 0, Integer.MAX_VALUE);
        RUIN_J_SPAWN_WEIGHT = builder.comment("Spawn weight for ruin J structure. [CE: 15.34_ruinJSpawnWeight]").defineInRange("ruinJSpawnWeight", 12, 0, Integer.MAX_VALUE);
        RADIO_SPAWN_WEIGHT = builder.comment("Spawn weight for radio structure. [CE: 15.35_radioSpawnWeight]").defineInRange("radioSpawnWeight", 25, 0, Integer.MAX_VALUE);
        FACTORY_SPAWN_WEIGHT = builder.comment("Spawn weight for factory structure. [CE: 15.36_factorySpawnWeight]").defineInRange("factorySpawnWeight", 40, 0, Integer.MAX_VALUE);
        PLAINS_NULL_WEIGHT = builder.comment("Null spawn weight for plains biome. [CE: 15.37_plainsNullWeight]").defineInRange("plainsNullWeight", 20, 0, Integer.MAX_VALUE);
        OCEAN_NULL_WEIGHT = builder.comment("Null spawn weight for ocean biomes. [CE: 15.38_oceanNullWeight]").defineInRange("oceanNullWeight", 35, 0, Integer.MAX_VALUE);
        CRANE_SPAWN_WEIGHT = builder.comment("Spawn weight for crane structure. [CE: 15.39_craneSpawnWeight]").defineInRange("craneSpawnWeight", 20, 0, Integer.MAX_VALUE);
        BROADCASTING_TOWER_SPAWN_WEIGHT = builder.comment("Spawn weight for broadcasting tower structure. [CE: 15.40_broadcastingTowerSpawnWeight]").defineInRange("broadcastingTowerSpawnWeight", 25, 0, Integer.MAX_VALUE);
        METEOR_DUNGEON_SPAWN_WEIGHT = builder.comment("Spawn weight for meteor dungeons. [CE: 15.41_meteorDungeonSpawnWeight]").defineInRange("meteorDungeonSpawnWeight", 1, 0, Integer.MAX_VALUE);

        ENABLE_DYNAMIC_STRUCTURE_SAVING = builder
                .comment("""
                        Whether dynamic structures scheduled for generation but that didn't meet generation requirements should be persisted to resume generation.
                        Affects small structures like ores, glyphid hives, flowers, etc. Will slightly increase world save size. [CE: 15.99_CE_01_enableDynamicStructureSaving]""")
                .define("enableDynamicStructureSaving", false);

        builder.pop();
    }

    /**
     * Returns CE's original {@code StructureConfig.enableStructures} int encoding
     * (1 = true, 0 = false, 2 = respect world flag) for mechanical compatibility with logic
     * ported from CE.
     */
    public static int enableStructuresFlag() {
        return switch (StructureFlag.valueOf(ENABLE_STRUCTURES.get().toUpperCase(Locale.ROOT))) {
            case TRUE -> 1;
            case FALSE -> 0;
            case WORLD_FLAG -> 2;
        };
    }

    /**
     * Mirrors CE's post-load {@code if(structureMinChunks > structureMaxChunks)} correction,
     * returning CE's corrected minimum (8) instead of the raw config value when the pair is
     * inverted, rather than mutating the stored config value.
     */
    public static int structureMinChunks() {
        return STRUCTURE_MIN_CHUNKS.get() > STRUCTURE_MAX_CHUNKS.get() ? 8 : STRUCTURE_MIN_CHUNKS.get();
    }

    public static int structureMaxChunks() {
        return STRUCTURE_MIN_CHUNKS.get() > STRUCTURE_MAX_CHUNKS.get() ? 24 : STRUCTURE_MAX_CHUNKS.get();
    }
}
