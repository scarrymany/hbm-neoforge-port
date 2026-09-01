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

import java.util.List;

/**
 * {@code NeoForgeRegistries.Keys.BIOME_MODIFIERS} bootstrap - final stage of the confirmed-real
 * four-stage {@code Feature -> ConfiguredFeature -> PlacedFeature -> BiomeModifier} pipeline (see
 * this port's own {@code OreBiomeModifiers} and neo-edition's confirmed-real
 * {@code NtmBiomeModifiers}). All four features are overworld-only in CE (oil bubble, bedrock-oil,
 * oil-sand and the ambient meteorite are never rolled for the nether or the end anywhere in
 * {@code HbmWorldGen}), bundled into a single {@link AddFeaturesBiomeModifier} the same way
 * {@code OreBiomeModifiers} bundles its own per-dimension roster - each entry already independently
 * self-gates via its own live config/biome read, so nothing is lost by sharing one modifier. Gated
 * {@code UNDERGROUND_ORES} to match neo-edition's own confirmed placement of its two oil features in
 * that same decoration step.
 */
public final class OilMeteorBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_OIL_METEOR_WORLDGEN = key("add_oil_meteor_worldgen");
    /** Landmine after vegetal so grass/trees do not overwrite the 1-block mine (CE {@code IWorldGenerator} is post-decorate). */
    public static final ResourceKey<BiomeModifier> ADD_LANDMINE_WORLDGEN = key("add_landmine_worldgen");
    /** Dud + waste barrel after vegetal — 1-block dud / surface tower, same CE post-decorate step. */
    public static final ResourceKey<BiomeModifier> ADD_DUD_BARREL_WORLDGEN = key("add_dud_barrel_worldgen");
    /** Spaceship wreck + satellite dish after vegetal — CE {@code IWorldGenerator} post-decorate. */
    public static final ResourceKey<BiomeModifier> ADD_SPACESHIP_SATELLITE_WORLDGEN = key("add_spaceship_satellite_worldgen");

    private OilMeteorBiomeModifiers() {
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        List<Holder<PlacedFeature>> features = List.of(
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.OIL_BUBBLE),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.BEDROCK_OIL_DEPOSIT),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.OIL_SAND_BUBBLE),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.METEORITE),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.ANTENNA),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.BUNKER),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.RADIO),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.SELLAFIELD),
                placedFeatures.getOrThrow(OilMeteorPlacedFeatures.NITAN_CHEST));

        context.register(ADD_OIL_METEOR_WORLDGEN, new AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD), HolderSet.direct(features), GenerationStep.Decoration.UNDERGROUND_ORES));
        context.register(ADD_LANDMINE_WORLDGEN, new AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(List.of(placedFeatures.getOrThrow(OilMeteorPlacedFeatures.LANDMINE))),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        context.register(ADD_DUD_BARREL_WORLDGEN, new AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(OilMeteorPlacedFeatures.DUD),
                        placedFeatures.getOrThrow(OilMeteorPlacedFeatures.BARREL))),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        context.register(ADD_SPACESHIP_SATELLITE_WORLDGEN, new AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(OilMeteorPlacedFeatures.SPACESHIP),
                        placedFeatures.getOrThrow(OilMeteorPlacedFeatures.SATELLITE))),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
    }

    private static ResourceKey<BiomeModifier> key(String path) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
