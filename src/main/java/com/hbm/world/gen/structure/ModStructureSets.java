package com.hbm.world.gen.structure;

import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.List;

/**
 * One set per CE biome lottery ({@code NTMWorldGenerator} + {@code StructureConfig} defaults).
 * Spacing 16 / separation 8 matches CE {@code structureMaxChunks=16}/{@code structureMinChunks=4}
 * padded so vanilla random-spread does not collapse (CE comment: settings lower than 8 may be
 * problematic). Weights are CE spawn-weight defaults.
 */
public final class ModStructureSets {

    public static final ResourceKey<StructureSet> SANDY = key("ntm_sandy");
    public static final ResourceKey<StructureSet> FLAT = key("ntm_flat");
    public static final ResourceKey<StructureSet> LAND = key("ntm_land");
    public static final ResourceKey<StructureSet> OCEAN = key("ntm_ocean");
    public static final ResourceKey<StructureSet> RUINS = key("ntm_ruins");
    public static final ResourceKey<StructureSet> METEOR = key("ntm_meteor");

    private ModStructureSets() {
    }

    public static void bootstrap(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        context.register(SANDY, new StructureSet(List.of(
                entry(structures, ModStructures.VERTIBIRD, 6),
                entry(structures, ModStructures.CRASHED_VERTIBIRD, 10),
                entry(structures, ModStructures.DESERT_SHACK_1, 18),
                entry(structures, ModStructures.DESERT_SHACK_2, 20),
                entry(structures, ModStructures.DESERT_SHACK_3, 22)
        ), spread(16, 8, 184018401)));

        context.register(FLAT, new StructureSet(List.of(
                entry(structures, ModStructures.RADIO_HOUSE, 25),
                entry(structures, ModStructures.SPIRE, 2),
                entry(structures, ModStructures.DISH, 10),
                entry(structures, ModStructures.LABORATORY, 20),
                entry(structures, ModStructures.FACTORY, 40),
                entry(structures, ModStructures.CRANE, 20),
                entry(structures, ModStructures.BROADCASTING_TOWER, 25)
        ), spread(16, 8, 184018402)));

        context.register(LAND, new StructureSet(List.of(
                entry(structures, ModStructures.FOREST_CHEM, 30),
                entry(structures, ModStructures.FOREST_POST, 30),
                entry(structures, ModStructures.PLANE_1, 25),
                entry(structures, ModStructures.PLANE_2, 25),
                entry(structures, ModStructures.BEACHED_PATROL, 15)
        ), spread(16, 8, 184018403)));

        context.register(OCEAN, new StructureSet(List.of(
                entry(structures, ModStructures.AIRCRAFT_CARRIER, 3),
                entry(structures, ModStructures.OIL_RIG, 5),
                entry(structures, ModStructures.LIGHTHOUSE, 1)
        ), spread(24, 12, 184018404)));

        context.register(RUINS, new StructureSet(List.of(
                entry(structures, ModStructures.RUIN_A, 10),
                entry(structures, ModStructures.RUIN_B, 12),
                entry(structures, ModStructures.RUIN_C, 12),
                entry(structures, ModStructures.RUIN_D, 12),
                entry(structures, ModStructures.RUIN_E, 12),
                entry(structures, ModStructures.RUIN_F, 12),
                entry(structures, ModStructures.RUIN_G, 12),
                entry(structures, ModStructures.RUIN_H, 12),
                entry(structures, ModStructures.RUIN_I, 12),
                entry(structures, ModStructures.RUIN_J, 12)
        ), spread(16, 8, 184018405)));

        context.register(METEOR, new StructureSet(
                structures.getOrThrow(ModStructures.METEOR_DUNGEON),
                spread(32, 16, 184018406)));
    }

    private static StructureSet.StructureSelectionEntry entry(HolderGetter<Structure> structures,
                                                              ResourceKey<Structure> key, int weight) {
        return new StructureSet.StructureSelectionEntry(structures.getOrThrow(key), weight);
    }

    private static RandomSpreadStructurePlacement spread(int spacing, int separation, int salt) {
        return new RandomSpreadStructurePlacement(spacing, separation, RandomSpreadType.LINEAR, salt);
    }

    private static ResourceKey<StructureSet> key(String path) {
        return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
