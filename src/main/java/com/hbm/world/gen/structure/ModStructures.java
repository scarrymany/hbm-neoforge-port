package com.hbm.world.gen.structure;

import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import java.util.Map;

/**
 * Datagen bootstrap for CE {@code NTMWorldGenerator} single-NBT structures. Heights/offsets from
 * {@code NTMWorldGenerator.java}:71-242 / meteor {@code minHeight=32} at line 261.
 */
public final class ModStructures {

    public static final ResourceKey<Structure> VERTIBIRD = key("vertibird");
    public static final ResourceKey<Structure> CRASHED_VERTIBIRD = key("crashed_vertibird");
    public static final ResourceKey<Structure> RADIO_HOUSE = key("radio_house");
    public static final ResourceKey<Structure> METEOR_DUNGEON = key("meteor_dungeon");
    public static final ResourceKey<Structure> SPIRE = key("spire");
    public static final ResourceKey<Structure> AIRCRAFT_CARRIER = key("aircraft_carrier");
    public static final ResourceKey<Structure> OIL_RIG = key("oil_rig");
    public static final ResourceKey<Structure> LIGHTHOUSE = key("lighthouse");
    public static final ResourceKey<Structure> BEACHED_PATROL = key("beached_patrol");
    public static final ResourceKey<Structure> DISH = key("dish");
    public static final ResourceKey<Structure> FOREST_CHEM = key("forest_chem");
    public static final ResourceKey<Structure> PLANE_1 = key("crashed_plane_1");
    public static final ResourceKey<Structure> PLANE_2 = key("crashed_plane_2");
    public static final ResourceKey<Structure> DESERT_SHACK_1 = key("desert_shack_1");
    public static final ResourceKey<Structure> DESERT_SHACK_2 = key("desert_shack_2");
    public static final ResourceKey<Structure> DESERT_SHACK_3 = key("desert_shack_3");
    public static final ResourceKey<Structure> LABORATORY = key("laboratory");
    public static final ResourceKey<Structure> FOREST_POST = key("forest_post");
    public static final ResourceKey<Structure> FACTORY = key("factory");
    public static final ResourceKey<Structure> CRANE = key("crane");
    public static final ResourceKey<Structure> BROADCASTING_TOWER = key("broadcasting_tower");
    public static final ResourceKey<Structure> RUIN_A = key("ntmruins_a");
    public static final ResourceKey<Structure> RUIN_B = key("ntmruins_b");
    public static final ResourceKey<Structure> RUIN_C = key("ntmruins_c");
    public static final ResourceKey<Structure> RUIN_D = key("ntmruins_d");
    public static final ResourceKey<Structure> RUIN_E = key("ntmruins_e");
    public static final ResourceKey<Structure> RUIN_F = key("ntmruins_f");
    public static final ResourceKey<Structure> RUIN_G = key("ntmruins_g");
    public static final ResourceKey<Structure> RUIN_H = key("ntmruins_h");
    public static final ResourceKey<Structure> RUIN_I = key("ntmruins_i");
    public static final ResourceKey<Structure> RUIN_J = key("ntmruins_j");

    private ModStructures() {
    }

    public static void bootstrap(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);
        HolderSet<Biome> ocean = biomes.getOrThrow(BiomeTags.IS_OCEAN);

        land(context, VERTIBIRD, overworld, "vertibird", -3, "sandy");
        land(context, CRASHED_VERTIBIRD, overworld, "crashed-vertibird", -10, "sandy");
        land(context, RADIO_HOUSE, overworld, "radio_house", -6, "flat");
        fixed(context, METEOR_DUNGEON, overworld, "meteor/meteor-core", 32, "land");
        land(context, SPIRE, overworld, "spire", -1, "flat");
        land(context, AIRCRAFT_CARRIER, ocean, "aircraft_carrier", -6, "ocean", "ocean");
        land(context, OIL_RIG, ocean, "oil_rig", -20, "ocean", "ocean");
        land(context, LIGHTHOUSE, overworld, "lighthouse", -40, "ocean_beach", "ocean");
        land(context, BEACHED_PATROL, overworld, "beached_patrol", -5, "beach");
        land(context, DISH, overworld, "dish", -10, "plains");
        land(context, FOREST_CHEM, overworld, "forest_chem", -9, "low");
        land(context, PLANE_1, overworld, "crashed_plane_1", -5, "low");
        land(context, PLANE_2, overworld, "crashed_plane_2", -8, "low");
        land(context, DESERT_SHACK_1, overworld, "desert_shack_1", -7, "sandy");
        land(context, DESERT_SHACK_2, overworld, "desert_shack_2", -7, "sandy");
        land(context, DESERT_SHACK_3, overworld, "desert_shack_3", -5, "sandy");
        land(context, LABORATORY, overworld, "laboratory", -10, "flat");
        land(context, FOREST_POST, overworld, "forest_post", -10, "low");
        land(context, FACTORY, overworld, "factory", -10, "flat");
        land(context, CRANE, overworld, "crane_mod", -9, "flat");
        land(context, BROADCASTING_TOWER, overworld, "broadcasting_tower", -9, "flat");
        ruin(context, RUIN_A, overworld, "ntmruins_a");
        ruin(context, RUIN_B, overworld, "ntmruins_b");
        ruin(context, RUIN_C, overworld, "ntmruins_c");
        ruin(context, RUIN_D, overworld, "ntmruins_d");
        ruin(context, RUIN_E, overworld, "ntmruins_e");
        ruin(context, RUIN_F, overworld, "ntmruins_f");
        ruin(context, RUIN_G, overworld, "ntmruins_g");
        ruin(context, RUIN_H, overworld, "ntmruins_h");
        ruin(context, RUIN_I, overworld, "ntmruins_i");
        ruin(context, RUIN_J, overworld, "ntmruins_j");
    }

    private static void land(BootstrapContext<Structure> context, ResourceKey<Structure> key,
                             HolderSet<Biome> biomes, String template, int offset, String gate) {
        land(context, key, biomes, template, offset, gate, "none");
    }

    private static void land(BootstrapContext<Structure> context, ResourceKey<Structure> key,
                             HolderSet<Biome> biomes, String template, int offset, String gate, String configGate) {
        context.register(key, new NbtPoiStructure(settings(biomes), template, offset, true, 0, gate, configGate));
    }

    private static void fixed(BootstrapContext<Structure> context, ResourceKey<Structure> key,
                              HolderSet<Biome> biomes, String template, int y, String gate) {
        context.register(key, new NbtPoiStructure(settings(biomes), template, 0, false, y, gate, "none"));
    }

    private static void ruin(BootstrapContext<Structure> context, ResourceKey<Structure> key,
                             HolderSet<Biome> biomes, String template) {
        context.register(key, new NbtPoiStructure(settings(biomes), template, -1, true, 0, "rain", "ruins"));
    }

    private static Structure.StructureSettings settings(HolderSet<Biome> biomes) {
        return new Structure.StructureSettings(biomes, Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.BEARD_THIN);
    }

    private static ResourceKey<Structure> key(String path) {
        return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
