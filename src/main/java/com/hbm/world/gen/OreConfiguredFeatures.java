package com.hbm.world.gen;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code Registries.CONFIGURED_FEATURE} bootstrap for every entry {@link OreWorldGenFeatures} filed
 * into its {@code OVERWORLD}/{@code NETHER}/{@code END} maps. Every one of the ~61 entries wraps its
 * own dedicated {@link Feature} instance in a bare {@link NoneFeatureConfiguration} - none of this
 * package's config data is baked into the datapack JSON (it stays a live, server-operator-editable
 * TOML value read fresh inside each {@code Feature.place()} call, per the research report's Key
 * design decision fork A) - so one small loop covers the entire roster instead of ~61 hand-written
 * {@code context.register(...)} calls.
 */
public final class OreConfiguredFeatures {

    /** {@code ConfiguredFeature} key for a given {@link OreWorldGenFeatures} registry name. */
    public static final Map<String, ResourceKey<ConfiguredFeature<?, ?>>> KEYS = new LinkedHashMap<>();

    private OreConfiguredFeatures() {
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Set<String> registered = new HashSet<>();
        registerAll(context, OreWorldGenFeatures.OVERWORLD, registered);
        registerAll(context, OreWorldGenFeatures.NETHER, registered);
        registerAll(context, OreWorldGenFeatures.END, registered);
    }

    private static void registerAll(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                     Map<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> group,
                                     Set<String> registered) {
        for (Map.Entry<String, DeferredHolder<Feature<?>, ? extends Feature<?>>> entry : group.entrySet()) {
            String name = entry.getKey();
            // Two dimension groups can share one Feature (e.g. bedrock_ore_overworld is filed into
            // both OVERWORLD and NETHER) - only register the ConfiguredFeature once.
            if (!registered.add(name)) continue;

            ResourceKey<ConfiguredFeature<?, ?>> key = KEYS.computeIfAbsent(name, OreConfiguredFeatures::key);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Feature<NoneFeatureConfiguration> feature = (Feature) entry.getValue().get();
            context.register(key, new ConfiguredFeature<>(feature, NoneFeatureConfiguration.INSTANCE));
        }
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
    }
}
