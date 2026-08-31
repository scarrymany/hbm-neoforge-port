package com.hbm.world.gen;

import com.hbm.main.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code Registries.PLACED_FEATURE} bootstrap for {@link OreConfiguredFeatures}' entire roster.
 * Every entry shares one {@link #STANDARD_ONCE_PER_CHUNK} placement-modifier list - a single
 * {@code InSquarePlacement.spread()} pick within the chunk plus a throwaway
 * {@code HeightRangePlacement} (mirroring neo-edition's own confirmed-real
 * {@code NtmPlacedFeatures.OIL_BUBBLE_PLACED} shape) - because every one of this package's
 * {@code Feature} implementations ignores the position it is handed and does its own internal
 * live-config-driven chunk-min/Y/attempt-count sampling; the modifier chain only needs to invoke
 * {@code place()} exactly once per chunk.
 */
public final class OrePlacedFeatures {

    private static final List<PlacementModifier> STANDARD_ONCE_PER_CHUNK = List.of(
            InSquarePlacement.spread(),
            HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(1)));

    public static final Map<String, ResourceKey<PlacedFeature>> KEYS = new LinkedHashMap<>();

    private OrePlacedFeatures() {
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        Set<String> registered = new HashSet<>();
        registerAll(context, configuredFeatures, OreWorldGenFeatures.OVERWORLD.keySet(), registered);
        registerAll(context, configuredFeatures, OreWorldGenFeatures.NETHER.keySet(), registered);
        registerAll(context, configuredFeatures, OreWorldGenFeatures.END.keySet(), registered);
    }

    private static void registerAll(BootstrapContext<PlacedFeature> context, HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures,
                                     Set<String> names, Set<String> registered) {
        for (String name : names) {
            if (!registered.add(name)) continue;

            // Computed directly rather than read back from OreConfiguredFeatures' own KEYS map -
            // RegistrySetBuilder does not guarantee bootstrap methods for different registries run in
            // any particular order (that is the entire point of HolderGetter's lazy resolution), so a
            // cross-class map populated as a side effect of another registry's bootstrap running first
            // would be a real, order-dependent bug. Both this method and
            // OreConfiguredFeatures.key(String) derive the same ResourceKey from the same name via the
            // same pure (registry, path) formula, so they always agree without needing to share state.
            ResourceKey<ConfiguredFeature<?, ?>> configuredKey = configuredFeatureKey(name);
            Holder<ConfiguredFeature<?, ?>> configuredHolder = configuredFeatures.getOrThrow(configuredKey);

            ResourceKey<PlacedFeature> key = KEYS.computeIfAbsent(name, OrePlacedFeatures::key);
            context.register(key, new PlacedFeature(configuredHolder, STANDARD_ONCE_PER_CHUNK));
        }
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredFeatureKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
    }
}
