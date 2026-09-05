package com.hbm.world.gen;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code Registries.CONFIGURED_FEATURE} bootstrap for {@link OilMeteorWorldGenFeatures}' roster - see
 * that class's own javadoc. Every entry wraps its {@link net.minecraft.world.level.levelgen.feature.Feature}
 * in a bare {@link NoneFeatureConfiguration}; none of this package's spawn-rate/radius data is baked
 * into datapack JSON (it stays a live, server-operator-editable {@code CompatibilityConfig} value read
 * fresh inside each {@code Feature.place()} call - see each {@code Feature}'s own javadoc).
 */
public final class OilMeteorConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_BUBBLE = key("oil_bubble");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BEDROCK_OIL_DEPOSIT = key("bedrock_oil_deposit");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_SAND_BUBBLE = key("oil_sand_bubble");
    public static final ResourceKey<ConfiguredFeature<?, ?>> METEORITE = key("meteorite");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ANTENNA = key("antenna");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BUNKER = key("bunker");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RADIO = key("radio");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SELLAFIELD = key("sellafield");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LANDMINE = key("landmine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NITAN_CHEST = key("nitan_chest");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUD = key("dud");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BARREL = key("barrel");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPACESHIP = key("spaceship");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATELLITE = key("satellite");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GLYPHID_HIVE = key("glyphid_hive");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DESERT_ATOM = key("desert_atom");

    private OilMeteorConfiguredFeatures() {
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(OIL_BUBBLE, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.OIL_BUBBLE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(BEDROCK_OIL_DEPOSIT, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.BEDROCK_OIL_DEPOSIT.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(OIL_SAND_BUBBLE, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.OIL_SAND_BUBBLE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(METEORITE, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.METEORITE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(ANTENNA, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.ANTENNA.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(BUNKER, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.BUNKER.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(RADIO, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.RADIO.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(SELLAFIELD, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.SELLAFIELD.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(LANDMINE, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.LANDMINE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(NITAN_CHEST, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.NITAN_CHEST.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(DUD, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.DUD.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(BARREL, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.BARREL.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(SPACESHIP, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.SPACESHIP.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(SATELLITE, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.SATELLITE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(GLYPHID_HIVE, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.GLYPHID_HIVE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(DESERT_ATOM, new ConfiguredFeature<>(OilMeteorWorldGenFeatures.DESERT_ATOM.get(), NoneFeatureConfiguration.INSTANCE));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
