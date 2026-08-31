package com.hbm.world.gen;

import com.hbm.main.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code NeoForgeRegistries.Keys.BIOME_MODIFIERS} bootstrap - the final stage of the confirmed-real
 * four-stage {@code Feature -> ConfiguredFeature -> PlacedFeature -> BiomeModifier} pipeline (see
 * {@code upstream/neo-edition}'s {@code NtmConfiguredFeatures}/{@code NtmPlacedFeatures}/
 * {@code NtmBiomeModifiers}, cross-referenced for API shape only). Rather than one
 * {@link AddFeaturesBiomeModifier} per placed feature (~61 entries), every dimension group's whole
 * roster is bundled into a single {@link HolderSet#direct} list and applied with one modifier per
 * group - {@code AddFeaturesBiomeModifier} accepts any number of placed features at once, and every
 * entry in a group already independently self-gates via its own live config read, so nothing is
 * lost by sharing one modifier.
 */
public final class OreBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_ORES = key("add_overworld_ores");
    public static final ResourceKey<BiomeModifier> ADD_NETHER_ORES = key("add_nether_ores");
    public static final ResourceKey<BiomeModifier> ADD_END_ORES = key("add_end_ores");

    private OreBiomeModifiers() {
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        register(context, ADD_OVERWORLD_ORES, biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                holders(placedFeatures, OreWorldGenFeatures.OVERWORLD));
        register(context, ADD_NETHER_ORES, biomes.getOrThrow(BiomeTags.IS_NETHER),
                holders(placedFeatures, OreWorldGenFeatures.NETHER));
        register(context, ADD_END_ORES, biomes.getOrThrow(BiomeTags.IS_END),
                holders(placedFeatures, OreWorldGenFeatures.END));
    }

    private static List<Holder<PlacedFeature>> holders(HolderGetter<PlacedFeature> placedFeatures,
                                                         Map<String, ?> group) {
        // Computed directly rather than read back from OrePlacedFeatures' own KEYS map -
        // RegistrySetBuilder does not guarantee bootstrap methods for different registries run in any
        // particular order, so a cross-class map populated as a side effect of another registry's
        // bootstrap running first would be a real, order-dependent bug. This derives the same
        // ResourceKey from the same name via the same pure (registry, path) formula
        // OrePlacedFeatures.key(String) uses, so both always agree without sharing state.
        List<Holder<PlacedFeature>> holders = new ArrayList<>(group.size());
        for (String name : group.keySet()) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
            holders.add(placedFeatures.getOrThrow(key));
        }
        return holders;
    }

    private static void register(BootstrapContext<BiomeModifier> context, ResourceKey<BiomeModifier> key,
                                  HolderSet<Biome> biomes, List<Holder<PlacedFeature>> features) {
        if (features.isEmpty()) return;
        context.register(key, new AddFeaturesBiomeModifier(biomes, HolderSet.direct(features), GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    private static ResourceKey<BiomeModifier> key(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
    }
}
