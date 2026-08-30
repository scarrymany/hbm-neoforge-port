package com.hbm.items.datagen;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.tool.ToolItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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

    /**
     * Vanilla's own {@code data/minecraft/tags/item/enchantable/{mining,durability}.json} already
     * nest {@code #minecraft:pickaxes}/{@code axes}/{@code shovels} as members (confirmed against the
     * Neo Edition reference's checked-in copies of those two files), so tagging an item into
     * {@link ItemTags#PICKAXES}/{@link ItemTags#AXES}/{@link ItemTags#SHOVELS} below already makes it
     * enchantable-mining/durability-eligible transitively. These two are still populated explicitly
     * in {@link #addToolTags()} anyway, mirroring the belt-and-suspenders style the confirmed real
     * {@code NtmItemTagProvider} (lines 107-108) uses for the equivalent
     * {@code ENCHANTABLE_MINING}/{@code ENCHANTABLE_MINING_LOOT} tags, so the generated tag files are
     * self-documenting rather than relying purely on vanilla's own nested-tag resolution. Hand-built
     * via {@link ItemTags#create} (same as that reference file does for every vanilla tag it touches)
     * rather than assumed as a same-named {@code ItemTags} constant field, since this port could not
     * verify that field exists against the real 1.21.1 jar in this sandbox.
     */
    private static final TagKey<Item> ENCHANTABLE_MINING = ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/mining"));
    private static final TagKey<Item> ENCHANTABLE_DURABILITY = ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/durability"));

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

        addToolTags();
    }

    /**
     * Seeds the vanilla tool tags for every {@code com.hbm.items.tool.ToolItems} entry that carries a
     * real {@link com.hbm.items.tool.ItemToolAbility.ToolRole} (i.e. every material-tiered mining
     * tool built on {@link com.hbm.items.tool.ItemToolAbility}/{@code ItemToolAbilityFueled}/
     * {@code ItemToolAbilityPower}, plus the chainsaw) - unlike the material-shape loop above, this
     * has no registry-derivable signal ({@code ToolRole} is a plain Java field, not part of the
     * registry name), so it is listed explicitly here, following the confirmed real pattern in
     * {@code NtmItemTagProvider.java} lines 55-108. {@code ToolRole.MINER} items act as a combined
     * pickaxe+shovel (see {@code ItemToolAbility#isEffectiveForState}/{@code #canPerformAction}) and
     * so go into both tags. {@code ItemMultitoolTool} instances ({@code multitool_dig}/{@code _silk})
     * have no {@code ToolRole} at all and are intentionally not tagged here.
     */
    private void addToolTags() {
        this.tag(ItemTags.PICKAXES).add(
                ToolItems.TITANIUM_PICKAXE.get(),
                ToolItems.STEEL_PICKAXE.get(),
                ToolItems.ALLOY_PICKAXE.get(),
                ToolItems.DESH_PICKAXE.get(),
                ToolItems.COBALT_PICKAXE.get(),
                ToolItems.COBALT_DECORATED_PICKAXE.get(),
                ToolItems.STARMETAL_PICKAXE.get(),
                ToolItems.CMB_PICKAXE.get(),
                ToolItems.SCHRABIDIUM_PICKAXE.get(),
                ToolItems.ELEC_PICKAXE.get(),
                // ToolRole.MINER (combined pickaxe+shovel effectiveness)
                ToolItems.CENTRI_STICK.get(),
                ToolItems.SMASHING_HAMMER.get(),
                ToolItems.BISMUTH_PICKAXE.get(),
                ToolItems.VOLCANIC_PICKAXE.get(),
                ToolItems.CHLOROPHYTE_PICKAXE.get(),
                ToolItems.MESE_PICKAXE.get(),
                ToolItems.DWARVEN_PICKAXE.get()
        );

        this.tag(ItemTags.AXES).add(
                ToolItems.TITANIUM_AXE.get(),
                ToolItems.STEEL_AXE.get(),
                ToolItems.ALLOY_AXE.get(),
                ToolItems.DESH_AXE.get(),
                ToolItems.COBALT_AXE.get(),
                ToolItems.COBALT_DECORATED_AXE.get(),
                ToolItems.STARMETAL_AXE.get(),
                ToolItems.CMB_AXE.get(),
                ToolItems.BISMUTH_AXE.get(),
                ToolItems.VOLCANIC_AXE.get(),
                ToolItems.CHLOROPHYTE_AXE.get(),
                ToolItems.SCHRABIDIUM_AXE.get(),
                ToolItems.MESE_AXE.get(),
                ToolItems.ELEC_AXE.get(),
                ToolItems.CHAINSAW.get()
        );

        this.tag(ItemTags.SHOVELS).add(
                ToolItems.TITANIUM_SHOVEL.get(),
                ToolItems.STEEL_SHOVEL.get(),
                ToolItems.ALLOY_SHOVEL.get(),
                ToolItems.DESH_SHOVEL.get(),
                ToolItems.COBALT_SHOVEL.get(),
                ToolItems.COBALT_DECORATED_SHOVEL.get(),
                ToolItems.STARMETAL_SHOVEL.get(),
                ToolItems.CMB_SHOVEL.get(),
                ToolItems.SCHRABIDIUM_SHOVEL.get(),
                ToolItems.ELEC_SHOVEL.get(),
                // ToolRole.MINER (combined pickaxe+shovel effectiveness)
                ToolItems.CENTRI_STICK.get(),
                ToolItems.SMASHING_HAMMER.get(),
                ToolItems.BISMUTH_PICKAXE.get(),
                ToolItems.VOLCANIC_PICKAXE.get(),
                ToolItems.CHLOROPHYTE_PICKAXE.get(),
                ToolItems.MESE_PICKAXE.get(),
                ToolItems.DWARVEN_PICKAXE.get()
        );

        this.tag(ENCHANTABLE_MINING).addTag(ItemTags.PICKAXES).addTag(ItemTags.AXES).addTag(ItemTags.SHOVELS);
        this.tag(ENCHANTABLE_DURABILITY).addTag(ItemTags.PICKAXES).addTag(ItemTags.AXES).addTag(ItemTags.SHOVELS);
    }
}
