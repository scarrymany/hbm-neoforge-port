package com.hbm.datagen;

import com.hbm.blocks.datagen.ModBlockLootTableProvider;
import com.hbm.blocks.datagen.ModBlockStateProvider;
import com.hbm.blocks.datagen.ModBlockTagProvider;
import com.hbm.damage.ModDamageTypes;
import com.hbm.damage.datagen.ModDamageTypeTagsProvider;
import com.hbm.items.datagen.ModItemModelProvider;
import com.hbm.items.datagen.ModItemTagProvider;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
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
 * 4.6): a fluid tag provider (Phase 0's fluid area), a sound definitions provider (Phase 0's sound
 * area) and a recipe provider (its own large content area).
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
    }
}
