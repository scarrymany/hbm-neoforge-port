package com.hbm.items.datagen;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Re-derives which {@code c:<tagFolder>/<material>} common item tags (see
 * {@link MaterialShapes#commonTag(NTMMaterial)}) actually apply, purely from
 * {@link Mats#orderedList} x {@link MaterialShapes#allShapes} plus whatever items are really
 * registered under {@link MainRegistry#MODID} at datagen time - never a hardcoded (material, shape)
 * list, so this needs zero coordination with whichever area registers a given material-shape item.
 *
 * <p>Only catches items registered under {@link MaterialShapes#buildRegistryName(NTMMaterial)}'s
 * exact naming convention (material-name suffixed with the shape token, e.g. {@code iron_ingot}).
 * CE-legacy hand-ported items that keep CE's original prefixed ids (e.g. {@code ingot_steel}) live
 * in a different naming scheme and are intentionally not matched here - see this area's final
 * report for why that split is expected, not a bug.
 */
public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
        super(output, provider, blockTags, MainRegistry.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (NTMMaterial mat : Mats.orderedList) {
            for (MaterialShapes shape : MaterialShapes.allShapes) {
                if (shape.registryName == null || shape.tagFolder == null) {
                    continue;
                }

                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, shape.buildRegistryName(mat));
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item != null) {
                    this.tag(shape.commonTag(mat)).add(item);
                }
            }
        }
    }
}
