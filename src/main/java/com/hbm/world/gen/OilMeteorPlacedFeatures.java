package com.hbm.world.gen;

import com.hbm.main.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

/**
 * {@code Registries.PLACED_FEATURE} bootstrap. Every entry supplies exactly one candidate position
 * per chunk ({@code InSquarePlacement.spread()} for X/Z) - the odds-of-actually-placing-anything roll
 * happens live inside each {@code Feature.place()} against a real {@code CompatibilityConfig} value
 * (see each {@code Feature}'s own javadoc), following this port's already-established
 * {@code OrePlacedFeatures} precedent and neo-edition's confirmed-real
 * {@code NtmPlacedFeatures.OIL_BUBBLE_PLACED} shape.
 * <p>
 * {@link #OIL_BUBBLE} uses {@code HeightRangePlacement.uniform(0, 24)} to match CE's own
 * {@code y = rand.nextInt(25)} underground-pocket range; the other three use
 * {@code HeightmapPlacement.onHeightmap(WORLD_SURFACE_WG)} since their own {@code place()} methods
 * compute their real working Y from the surface height (bedrock-oil ignores it entirely in favor of
 * {@code level.getMinBuildHeight()}; oil-sand and meteorite both start from the surface value directly).
 */
public final class OilMeteorPlacedFeatures {

    private static final List<PlacementModifier> OIL_BUBBLE_MODIFIERS = List.of(
            InSquarePlacement.spread(),
            HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(24)));
    private static final List<PlacementModifier> SURFACE_MODIFIERS = List.of(
            InSquarePlacement.spread(),
            HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG));

    public static final ResourceKey<PlacedFeature> OIL_BUBBLE = key("oil_bubble");
    public static final ResourceKey<PlacedFeature> BEDROCK_OIL_DEPOSIT = key("bedrock_oil_deposit");
    public static final ResourceKey<PlacedFeature> OIL_SAND_BUBBLE = key("oil_sand_bubble");
    public static final ResourceKey<PlacedFeature> METEORITE = key("meteorite");
    public static final ResourceKey<PlacedFeature> ANTENNA = key("antenna");
    public static final ResourceKey<PlacedFeature> BUNKER = key("bunker");
    public static final ResourceKey<PlacedFeature> RADIO = key("radio");

    private OilMeteorPlacedFeatures() {
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, OIL_BUBBLE, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.OIL_BUBBLE), OIL_BUBBLE_MODIFIERS);
        register(context, BEDROCK_OIL_DEPOSIT, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.BEDROCK_OIL_DEPOSIT), SURFACE_MODIFIERS);
        register(context, OIL_SAND_BUBBLE, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.OIL_SAND_BUBBLE), SURFACE_MODIFIERS);
        register(context, METEORITE, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.METEORITE), SURFACE_MODIFIERS);
        register(context, ANTENNA, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.ANTENNA), SURFACE_MODIFIERS);
        register(context, BUNKER, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.BUNKER), SURFACE_MODIFIERS);
        register(context, RADIO, configuredFeatures.getOrThrow(OilMeteorConfiguredFeatures.RADIO), SURFACE_MODIFIERS);
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                  Holder<ConfiguredFeature<?, ?>> configured, List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configured, modifiers));
    }

    private static ResourceKey<PlacedFeature> key(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
