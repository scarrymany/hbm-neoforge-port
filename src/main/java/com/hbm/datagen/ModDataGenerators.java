package com.hbm.datagen;

import com.hbm.blocks.datagen.ModBlockLootTableProvider;
import com.hbm.blocks.datagen.ModBlockStateProvider;
import com.hbm.blocks.datagen.ModBlockTagProvider;
import com.hbm.damage.ModDamageTypes;
import com.hbm.damage.datagen.ModDamageTypeTagsProvider;
import com.hbm.entity.mob.CreeperVariantBiomeModifiers;
import com.hbm.items.datagen.ModItemModelProvider;
import com.hbm.items.datagen.ModItemTagProvider;
import com.hbm.main.MainRegistry;
import com.hbm.world.biome.ModCraterBiomes;
import com.hbm.world.gen.OilMeteorBiomeModifiers;
import com.hbm.world.gen.OilMeteorConfiguredFeatures;
import com.hbm.world.gen.OilMeteorPlacedFeatures;
import com.hbm.world.gen.OreBiomeModifiers;
import com.hbm.world.gen.OreConfiguredFeatures;
import com.hbm.world.gen.OrePlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The single {@link GatherDataEvent} subscriber wiring every Phase 1 datagen provider together,
 * plus the damage-type registry/tags Phase 0 built but never registered here (see
 * {@code docs/phase0/damage_types.md} and {@code docs/phase0/DIGEST.md}, both of which flagged this
 * exact gap). Confirmed real design, mirroring the Neo Edition reference's {@code NtmDataGenerators}.
 *
 * <p>This class only imports and wires other providers - it never needs to change as Phase 1's
 * item/block count grows, only the individual provider classes do. Slots deliberately left out
 * because they are out of this area's scope (see {@code docs/phase1/datagen_framework.md} section
 * 4.6): a fluid tag provider (Phase 0's fluid area) and a sound definitions provider (Phase 0's
 * sound area). The recipe-provider slot that section flagged as "its own large content area" is
 * filled as of task c16-recipe-datagen by {@link ModRecipeProvider} - see that class's own javadoc
 * for exactly how much of CE's ~1,900+ vanilla-crafting-recipe corpus it covers (a first,
 * explicitly-scoped slice, not the whole corpus).
 */
// bus = Bus.MOD required: GatherDataEvent implements IModBusEvent and only fires on the mod bus -
// @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent (confirmed
// against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder();
        registrySetBuilder.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);
        registrySetBuilder.add(Registries.BIOME, ModCraterBiomes::bootstrap);
        // Phase 4 ore-vein/bedrock-ore world-gen (docs/phase4/ore_veins_and_bedrock_ores.md) - the
        // three remaining stages of the Feature -> ConfiguredFeature -> PlacedFeature -> BiomeModifier
        // pipeline (Feature instances themselves are registered separately via
        // OreWorldGenFeatures.register(modEventBus), like any other DeferredRegister).
        // One bootstrap per registry key — RegistrySetBuilder.add overwrites a previous add for
        // the same key, so Ore + OilMeteor + creeper modifiers must share a single lambda.
        registrySetBuilder.add(Registries.CONFIGURED_FEATURE, context -> {
            OreConfiguredFeatures.bootstrap(context);
            OilMeteorConfiguredFeatures.bootstrap(context);
        });
        registrySetBuilder.add(Registries.PLACED_FEATURE, context -> {
            OrePlacedFeatures.bootstrap(context);
            OilMeteorPlacedFeatures.bootstrap(context);
        });
        registrySetBuilder.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
            OreBiomeModifiers.bootstrap(context);
            OilMeteorBiomeModifiers.bootstrap(context);
            CreeperVariantBiomeModifiers.bootstrap(context);
        });
        DatapackBuiltinEntriesProvider datapackProvider =
                new DatapackBuiltinEntriesProvider(output, lookup, registrySetBuilder, Set.of(MainRegistry.MODID));
        generator.addProvider(event.includeServer(), datapackProvider);

        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, helper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));

        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(output, lookup, helper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(),
                new ModItemTagProvider(output, lookup, blockTagsProvider.contentsGetter(), helper));
        generator.addProvider(event.includeServer(), new ModDamageTypeTagsProvider(output, lookup, helper));

        LootTableProvider.SubProviderEntry blockLootSubProvider =
                new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK);
        generator.addProvider(event.includeServer(),
                (DataProvider.Factory<LootTableProvider>) lootOutput ->
                        new LootTableProvider(lootOutput, Collections.emptySet(), List.of(blockLootSubProvider), lookup));

        // c16-recipe-datagen: highest-value slice of CE's vanilla-crafting-table recipe corpus
        // (ToolRecipes/ArmorRecipes/MineralRecipes) - see ModRecipeProvider's own class javadoc for
        // exactly what is and is not covered.
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
    }
}
