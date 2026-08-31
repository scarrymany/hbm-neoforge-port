package com.hbm.entity.mob;

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
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code NeoForgeRegistries.Keys.BIOME_MODIFIERS} bootstrap for this package's 3 naturally-spawning
 * CE creeper variants (Gold/Volatile/Phosgene - see {@code docs/phase4/entities_creeper_variants.md}'s
 * Headline finding #2: Tainted/Nuclear are mutation-only in CE and never appear in any spawn list).
 * Follows the same real, already-compiling {@code BootstrapContext<BiomeModifier>} pipeline shape as
 * {@code com.hbm.world.gen.OreBiomeModifiers} (this port's own precedent for exactly this pattern,
 * consulted for API shape) - the {@code net.neoforged.neoforge.common.world.BiomeModifiers} nested
 * modifier records used there ({@code AddFeaturesBiomeModifier}) and here
 * ({@link AddSpawnsBiomeModifier}) are siblings of the same small NeoForge utility class.
 * <p>
 * <b>Data</b>: CE's {@code EntityMappings.writeSpawns()} (read in full) registers exactly
 * Phosgene weight 5, Volatile weight 10, Gold weight 1, all with {@code minGroupCount=maxGroupCount=1}
 * (no pack spawning), across every biome except mushroom islands - reproduced exactly here via
 * {@link BiomeTags#IS_OVERWORLD} minus {@link Biomes#MUSHROOM_FIELDS} (the modern single-biome
 * successor to 1.12's {@code BiomeMushroomIsland} check).
 * <p>
 * <b>Known gap - the Y&lt;=40+Overworld placement restriction is not enforced here.</b> Reading CE's
 * real source directly (not just the research report's own summary table, which named this
 * restriction for Gold alone) shows <em>all three</em> of these classes override
 * {@code getCanSpawnHere() -> super.getCanSpawnHere() && posY<=40 && dimension==0} identically - a
 * real correction to the report's table, noted here and in this task's own findings. {@code
 * getCanSpawnHere()} has no direct 1.21.1 analogue; the modern per-position placement gate is
 * {@code net.minecraft.world.entity.SpawnPlacements.register(...)}, which has zero precedent
 * anywhere in this port or Neo Edition to verify NeoForge 1.21.1's exact registration signature
 * against (unlike this file's own {@code AddSpawnsBiomeModifier} half, which does have a real,
 * already-compiling precedent). Per this task's own explicit guidance to prefer a documented
 * {@code knownGap} over guessing an unverifiable, mod-wide-compile-risking API shape, that call is
 * intentionally not made here: all 3 variants will naturally spawn at any Y level in the Overworld
 * (via this biome modifier and their {@code MobCategory.MONSTER} attribute registration) rather than
 * being restricted to Y&lt;=40, until someone confirms the right {@code SpawnPlacements} call against
 * a real compiled jar.
 */
public final class CreeperVariantBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_CREEPER_VARIANT_SPAWNS = key("add_creeper_variant_spawns");

    private CreeperVariantBiomeModifiers() {
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        List<Holder<Biome>> overworldExceptMushroom = new ArrayList<>();
        for (Holder<Biome> holder : biomes.getOrThrow(BiomeTags.IS_OVERWORLD)) {
            if (!holder.is(Biomes.MUSHROOM_FIELDS)) {
                overworldExceptMushroom.add(holder);
            }
        }

        context.register(ADD_CREEPER_VARIANT_SPAWNS, new AddSpawnsBiomeModifier(
                HolderSet.direct(overworldExceptMushroom),
                List.of(
                        new MobSpawnSettings.SpawnerData(CreeperVariantEntityTypes.CREEPER_PHOSGENE.get(), 5, 1, 1),
                        new MobSpawnSettings.SpawnerData(CreeperVariantEntityTypes.CREEPER_VOLATILE.get(), 10, 1, 1),
                        new MobSpawnSettings.SpawnerData(CreeperVariantEntityTypes.CREEPER_GOLD.get(), 1, 1, 1)
                )));
    }

    private static ResourceKey<BiomeModifier> key(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name));
    }
}
