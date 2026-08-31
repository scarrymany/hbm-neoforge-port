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

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Re-derives which {@code c:<tagFolder>/<material>} common item tags (see
 * {@link MaterialShapes#commonTag(NTMMaterial)}) actually apply, purely from
 * {@link Mats#orderedList} x {@link MaterialShapes#allShapes} plus whatever items are really
 * registered under {@link MainRegistry#MODID} at datagen time - never a hardcoded (material, shape)
 * list, so this needs zero coordination with whichever area registers a given material-shape item.
 *
 * <p>Two independent passes populate these tags:
 * <ul>
 *     <li>{@link #addTags(HolderLookup.Provider)}'s main loop catches items registered under
 *     {@link MaterialShapes#buildRegistryName(NTMMaterial)}'s exact naming convention (material-name
 *     suffixed with the shape token, e.g. {@code iron_ingot}) - this is the convention
 *     {@code MaterialItemGenerator}/{@code MaterialBlockGenerator} use.</li>
 *     <li>{@link #addLegacyMaterialTags()} catches CE-legacy hand-ported items that instead keep
 *     CE's original prefixed ids (e.g. {@code ingot_steel}, {@code powder_iron}, {@code
 *     plate_titanium}) - the naming scheme {@code IngotNuggetItems}/{@code BilletPowderItems}/
 *     {@code PlateCrystalWasteItems} use. Without this second pass none of those ~460 items would
 *     carry any {@code c:<tagFolder>/<material>} tag at all, and {@link Mats#getMaterialsFromItem}'s
 *     generic ("Tier 1") auto-smelt lookup - the mechanism that lets a crucible turn e.g.
 *     {@code ingot_titanium} back into 1 titanium ingot's worth of material with no per-item
 *     {@code MatDistribution} entry, exactly like CE - would silently never fire for them. See
 *     {@code docs/phase7/crucible_matdistribution.md}'s "two-tier lookup" section for the full
 *     finding this pass closes.</li>
 * </ul>
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

        addLegacyMaterialTags();
        addToolTags();
    }

    /**
     * Legacy prefix -> autogen shape pairs for CE's hand-named {@code <prefix>_<material>} resource
     * item families ({@code IngotNuggetItems}/{@code BilletPowderItems}/{@code
     * PlateCrystalWasteItems}), matched positionally against {@link #LEGACY_SHAPES}. {@code powder_}
     * maps to {@link MaterialShapes#DUST} (CE's "powder" is this port's "dust" shape) rather than a
     * same-named shape - every other prefix already matches its shape's own {@link
     * MaterialShapes#registryName} token.
     */
    private static final String[] LEGACY_PREFIXES = {"ingot", "nugget", "billet", "powder", "plate", "crystal"};
    private static final MaterialShapes[] LEGACY_SHAPES = {
            MaterialShapes.INGOT, MaterialShapes.NUGGET, MaterialShapes.BILLET,
            MaterialShapes.DUST, MaterialShapes.PLATE, MaterialShapes.CRYSTAL,
    };

    /**
     * Closes the gap documented in this class's own javadoc: for every {@code (material, legacy
     * shape)} pair, tries every one of that material's alias names (not just its canonical {@link
     * NTMMaterial#getRegistryName()}) under every legacy prefix, e.g. {@code ingot_th232} for
     * material {@code Thorium232} - CE's own field/id there uses the material's second alias
     * ({@code Th232}), not its canonical first name, exactly the kind of per-material naming quirk
     * that makes trying every alias necessary rather than just the canonical one. Stops at the first
     * alias that resolves to a real registered item per (material, shape) pair, so one real item is
     * never tagged twice into the same tag.
     */
    private void addLegacyMaterialTags() {
        for (NTMMaterial mat : Mats.orderedList) {
            for (int i = 0; i < LEGACY_PREFIXES.length; i++) {
                String prefix = LEGACY_PREFIXES[i];
                MaterialShapes shape = LEGACY_SHAPES[i];

                for (String alias : mat.names) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                            MainRegistry.MODID, prefix + "_" + alias.toLowerCase(Locale.US));
                    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                    if (item != null) {
                        this.tag(shape.commonTag(mat)).add(item);
                        break;
                    }
                }
            }
        }
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
