package com.hbm.world.biome;

import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

/**
 * Data-driven replacement for CE's {@code com.hbm.world.biome.BiomeGenCraterBase} (111 lines, read
 * in full) - 3 cosmetic-only {@link Biome} entries ({@code crater}/{@code crater_inner}/
 * {@code crater_outer}) with no unique mob spawns or world-gen features of their own, per
 * docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 3: "does not need any placement/world-gen
 * logic of its own - it is only ever assigned dynamically" (by
 * {@code EntityFalloutRain.getBiomeChangeKey()}/{@code paintBiome()}, and consumed by
 * {@code com.hbm.handler.EntityEffectHandler#handleCraterRadiation} - both landed later in this same
 * Phase 4 content wave). This class only registers the 3 biomes - no placement logic is added here.
 * <p>
 * Bootstrap shape ({@code BootstrapContext<Biome>}, the {@code HolderGetter} lookups needed for an
 * empty {@link BiomeGenerationSettings}, {@code EnumProxy}-injected custom
 * {@link GrassColorModifier} constants for CE's real per-position noise-based grass tint) is the
 * confirmed-real, actually-compiling NeoForge 1.21.1 API shape from {@code upstream/neo-edition}'s
 * own {@code com.hbm.registry.NtmBiomes} - reused here for API shape only, per this task's ground
 * rules (neo-edition is never a source of behavior/numbers).
 * <p>
 * <b>Every color/noise-threshold number below is independently re-verified against CE's own real
 * {@code BiomeGenCraterBase.java}, not copied from neo-edition</b>: the 3 subclasses'
 * {@code getGrassColorAtPos} noise thresholds/RGB pairs and {@code getFoliageColorAtPos} flat
 * constants (identical {@code 0x6A7039} for all three) match neo-edition's own values exactly, but
 * neo-edition's {@code waterColor} guess ({@code 0x10161C} for all three) does <b>not</b> match CE's
 * real, single, base-class-level {@code getWaterColorMultiplier() -> 0xE0FFAE} override (defined once
 * on {@code BiomeGenCraterBase} itself, never overridden per-subclass, so all three genuinely share
 * one water color in CE) - corrected here to CE's actual value rather than carried over wrong.
 * <p>
 * {@code waterFogColor}/{@code fogColor} have no CE override in this file at all (1.12's {@code Biome}
 * never required either); {@link BiomeSpecialEffects.Builder} requires both non-null in 1.21.1, so -
 * matching neo-edition's own already-real fallback shape for this same "field CE never set" gap -
 * they reuse the one CE color each effects group already has ({@code waterFogColor = waterColor},
 * {@code fogColor} = the per-biome sky color). {@code temperature}/{@code downfall} are likewise never
 * set by CE's {@code new Biome.BiomeProperties("Crater")} calls (only {@code .setRainDisabled()} is
 * chained) - CE 1.12's {@code BiomeProperties} defaults both to {@code 0.5F} when unset
 * (well-established 1.12 vanilla default, not independently verified against a decompiled jar in this
 * sandbox - flagged per this task's ground rules), used here rather than neo-edition's own unexplained
 * {@code 0.8F}/{@code 0.0F} guess. Neither value has any observable gameplay effect regardless, since
 * {@code hasPrecipitation} is already forced off and every mob-spawn list is left empty (mirroring
 * CE's constructor clearing {@code spawnableCreatureList}/{@code spawnableWaterCreatureList}/
 * {@code spawnableCaveCreatureList}).
 * <p>
 * <b>Not ported</b>: CE's {@code BiomeDictionary.addTypes(..., DRY, DEAD, WASTELAND)} calls - modern
 * biome tags are a separate datapack-tag datagen concern (no {@code BiomeTagsProvider} exists in this
 * port yet, and no confirmed-real NeoForge convention tag names for "dry"/"dead"/"wasteland" were
 * found anywhere in this port or neo-edition to map CE's Forge-1.12 {@code BiomeDictionary.Type} enum
 * onto) - flagged as a small, low-value follow-up gap, since this report itself says these biomes need
 * no placement logic and nothing in this port currently queries biome tags for crater-adjacent
 * behavior.
 */
@SuppressWarnings("removal") // EnumProxy - same annotation neo-edition's own real, compiling NtmBiomes needs for it
public final class ModCraterBiomes {

    public static final ResourceKey<Biome> CRATER = key("crater");
    public static final ResourceKey<Biome> CRATER_INNER = key("crater_inner");
    public static final ResourceKey<Biome> CRATER_OUTER = key("crater_outer");

    /** CE's single, shared {@code BiomeGenCraterBase.getWaterColorMultiplier()} override. */
    private static final int WATER_COLOR = 0xE0FFAE;

    private ModCraterBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // EnumProxy.getValue() needs META-INF/enumextensions.json (not shipped). Vanilla NONE
        // until that file lands; CE noise-tinted grass is visual-only.
        context.register(CRATER, baseBuilder(placed, carvers)
                .specialEffects(effects(0x525A52, GrassColorModifier.NONE))
                .build());
        context.register(CRATER_INNER, baseBuilder(placed, carvers)
                .specialEffects(effects(0x424A42, GrassColorModifier.NONE))
                .build());
        context.register(CRATER_OUTER, baseBuilder(placed, carvers)
                .specialEffects(effects(0x6B9189, GrassColorModifier.NONE))
                .build());
    }

    private static Biome.BiomeBuilder baseBuilder(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.5F)
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.Builder(placed, carvers).build());
    }

    /** CE's {@code getFoliageColorAtPos} constant is {@code 0x6A7039} for all three subclasses alike. */
    private static BiomeSpecialEffects effects(int skyColor, GrassColorModifier grassMod) {
        return new BiomeSpecialEffects.Builder()
                .waterColor(WATER_COLOR)
                .waterFogColor(WATER_COLOR)
                .foliageColorOverride(0x6A7039)
                .grassColorModifier(grassMod)
                .skyColor(skyColor)
                .fogColor(skyColor)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .build();
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    // CE's exact per-position grass noise thresholds (BiomeGenCrater/-Inner/-Outer#getGrassColorAtPos:
    // `GRASS_COLOR_NOISE.getValue(x * 0.225D, z * 0.225D) < -0.1D ? a : b`), injected as real
    // GrassColorModifier enum constants via NeoForge's EnumProxy - confirmed real shape, this exact
    // idiom already compiling in neo-edition's own NtmBiomes. The incoming `color` argument is ignored,
    // matching CE's own override (which never reads a "base" color either).
    public static final EnumProxy<GrassColorModifier> CRATER_GRASS_MOD = new EnumProxy<>(
            GrassColorModifier.class, MainRegistry.MODID + ":crater_grass_mod",
            (GrassColorModifier.ColorModifier) (x, z, color) ->
                    Biome.BIOME_INFO_NOISE.getValue(x * 0.225, z * 0.225, false) < -0.1 ? 0x606060 : 0x505050);

    public static final EnumProxy<GrassColorModifier> CRATER_INNER_GRASS_MOD = new EnumProxy<>(
            GrassColorModifier.class, MainRegistry.MODID + ":crater_inner_grass_mod",
            (GrassColorModifier.ColorModifier) (x, z, color) ->
                    Biome.BIOME_INFO_NOISE.getValue(x * 0.225, z * 0.225, false) < -0.1 ? 0x404040 : 0x303030);

    public static final EnumProxy<GrassColorModifier> CRATER_OUTER_GRASS_MOD = new EnumProxy<>(
            GrassColorModifier.class, MainRegistry.MODID + ":crater_outer_grass_mod",
            (GrassColorModifier.ColorModifier) (x, z, color) ->
                    Biome.BIOME_INFO_NOISE.getValue(x * 0.225, z * 0.225, false) < -0.1 ? 0x776F59 : 0x6F6752);
}
