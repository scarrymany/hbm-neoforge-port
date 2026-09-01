package com.hbm.inventory.material;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static com.hbm.inventory.material.Mats.*;
import static com.hbm.inventory.material.MaterialShapes.*;

/**
 * Ports CE's {@code MatDistribution.registerDefaults()} (upstream hbm-ce
 * {@code com.hbm.inventory.material.MatDistribution.java} lines 74-136) - the curated table of
 * "this item/ore yields these crucible materials" entries, feeding {@link Mats#registerEntry}/
 * {@link Mats#registerOre}. See {@code docs/phase7/crucible_matdistribution.md} for the full
 * per-entry catalog this class transcribes.
 *
 * <p><b>Not ported: CE's JSON round-trip.</b> CE's {@code MatDistribution} extends
 * {@code SerializableRecipe}, whose {@code initialize()} only reads an admin-dropped
 * {@code config/hbmRecipes/hbmCrucibleSmelting.json} override file if one exists on disk; the
 * shipped-mod default state (what every player actually sees) is 100% the Java
 * {@code registerDefaults()} below. That live-server-admin customization/persistence layer is not
 * carried forward here, matching this port's existing precedent for {@code HazmatRegistry} (see
 * that class's own javadoc) - a deliberate scope decision, not an oversight.
 *
 * <p><b>Exact-item resolution, not tag-path.</b> CE's {@code registerOre(String, ...)} keys off a
 * live Forge ore-dictionary name computed at lookup time; this port's {@link Mats#registerOre}
 * keys off a NeoForge item *tag path* instead (see {@link Mats#getMaterialsFromItem}). Rather than
 * assume a specific {@code c:<tag>} happens to already apply to a given vanilla/modded item at
 * runtime (a datagen concern this class has no visibility into), every entry below - including the
 * ones CE itself expressed as an ore-dict key (stone/cobblestone/every ore) - is registered via
 * {@link Mats#registerEntry} against the one real, resolved {@link Item} instead. This is strictly
 * more deterministic than a tag-path match and behaviorally equivalent for every entry actually
 * covered here (each currently has exactly one real backing item in this port), at the cost of not
 * automatically covering a hypothetical second same-tagged item a later phase might add - an
 * acceptable, documented simplification.
 *
 * <p><b>Mod-item resolution.</b> Several target items (ore blocks, {@code MachineItems} castables,
 * {@code stone_resource_*}, {@code powder_flux}) have no public {@code DeferredItem} field this
 * class can reference directly (registered via a private per-class {@code reg(name, ...)} helper
 * with no exposed accessor). Rather than add a cross-cutting public accessor to another area's file
 * (out of this class's edit scope), every such item is resolved by its known registry id string via
 * {@link BuiltInRegistries#ITEM}, exactly the pattern {@code ModItemTagProvider.addTags()} already
 * uses. This runs from {@code CommonEvents.commonSetup}'s {@code FMLCommonSetupEvent.enqueueWork}
 * block (see the wiring note in this class's own javadoc below), i.e. strictly after every
 * {@code RegisterEvent} has fired, so every id below is guaranteed already registered by the time
 * {@link #entry(String, Object...)} resolves it.
 *
 * <p><b>Wiring (not performed by this class itself):</b> {@link #registerAll()} must be called once
 * from {@code CommonEvents.commonSetup}'s {@code enqueueWork} block, alongside
 * {@code RefineryRecipes.registerRefinery()} et al. - see that method's own in-line comment for why
 * this timing is required. {@code CommonEvents.java} is a shared file this task does not edit
 * directly (see this task's wiringSnippets output); a coordinator applies that one-line addition.
 *
 * <p><b>What is NOT here:</b> 11 of CE's 41 curated entries (plus a 12th, partial, entry) are
 * blocked on a missing {@code Item}/{@code Block} registration in a different area and are left as
 * commented-out TODOs below, each citing exactly what is missing - see this class's own inline
 * comments and {@code docs/phase7/crucible_matdistribution.md}'s "Item/registry dependency check"
 * section. None of them need new recipe logic, only a future item registration to reconnect.
 */
public final class MatDistributionDefaults {

    private static boolean registered = false;

    private MatDistributionDefaults() {
    }

    public static void registerAll() {
        if (registered) return;
        registered = true;

        // ==================== vanilla blocks/items (CE MatDistribution.java lines 76-82, 131) ====================

        // #1 registerOre("stone", ...) / #2 registerOre("cobblestone", ...): CE keys these off the
        // "stone"/"cobblestone" ore-dict names (which cover every stone-like block across every mod);
        // this port targets the one real vanilla item directly (see class javadoc).
        registerEntry(Blocks.STONE.asItem(), MAT_STONE, BLOCK.q(1));
        registerEntry(Blocks.COBBLESTONE.asItem(), MAT_STONE, BLOCK.q(1));

        // #3
        registerEntry(Blocks.OBSIDIAN.asItem(), MAT_OBSIDIAN, BLOCK.q(1));

        // #4: "6 ingots used, 16 rails produced" - MaterialShapes.q(unitsUsed, itemsProduced).
        registerEntry(Blocks.RAIL.asItem(), MAT_IRON, INGOT.q(6, 16));

        // #5
        registerEntry(Blocks.POWERED_RAIL.asItem(), MAT_GOLD, INGOT.q(6, 6), MAT_REDSTONE, DUST.q(1, 6));

        // #6
        registerEntry(Blocks.DETECTOR_RAIL.asItem(), MAT_IRON, INGOT.q(6, 6), MAT_REDSTONE, DUST.q(1, 6));

        // #7 (CE registers this twice, byte-for-byte identical - one logical entry)
        registerEntry(Items.MINECART, MAT_IRON, INGOT.q(5));

        // #27 registerOre(COAL.ore(), ...): CE's COAL DictFrame has no explicit .ore() call, so it
        // only ever matches Forge's auto-registered vanilla "oreCoal" - i.e. vanilla coal ore only.
        registerEntry(Blocks.COAL_ORE.asItem(), MAT_CARBON, GEM.q(3), MAT_STONE, QUART.q(1));

        // #35 registerOre(REDSTONE.ore(), ...): same reasoning - vanilla redstone ore only.
        registerEntry(Blocks.REDSTONE_ORE.asItem(), MAT_REDSTONE, INGOT.q(4), MAT_STONE, QUART.q(1));

        // #40: CE registers new ItemStack(Items.COAL, 1, 1) (the 1.12 metadata charcoal variant).
        // 1.21 gives charcoal its own distinct item id - no metadata/flattening needed at all.
        registerEntry(Items.CHARCOAL, MAT_CARBON, NUGGET.q(3));

        // ==================== MachineItems castables (CE lines 87, 89-93) ====================

        // #10/#11: blades_steel/blades_titanium (MachineItems.registerBlades()). blade_titanium/
        // blade_tungsten (CE plain ItemBase items, #8/#9) are not registered by that area - see
        // "still blocked" below.
        entry("blades_steel", MAT_STEEL, INGOT.q(4));
        entry("blades_titanium", MAT_TITANIUM, INGOT.q(4));

        // #12-#16: stamp_*_flat (MachineItems.registerStamps()).
        entry("stamp_stone_flat", MAT_STONE, INGOT.q(3));
        entry("stamp_iron_flat", MAT_IRON, INGOT.q(3));
        entry("stamp_steel_flat", MAT_STEEL, INGOT.q(3));
        entry("stamp_titanium_flat", MAT_TITANIUM, INGOT.q(3));
        entry("stamp_obsidian_flat", MAT_OBSIDIAN, INGOT.q(3));

        // ==================== OreBlocks ores (CE lines 108-126) ====================
        // Every alias below is a distinct real Item this port registers under OreBlocks.ore(name)/
        // outgas(name) (each also gets its own BlockItem of the same registry name) - CE bound all
        // aliases of one ore family to the identical material yield via a single ore-dict key; this
        // port instead registers each alias item individually with that same yield (see class
        // javadoc on exact-item vs tag-path resolution).

        // #23
        entry("ore_gneiss_iron", MAT_IRON, INGOT.q(2), MAT_TITANIUM, NUGGET.q(3), MAT_STONE, QUART.q(1));

        // #24
        entry("ore_titanium", MAT_TITANIUM, INGOT.q(2), MAT_IRON, NUGGET.q(3), MAT_STONE, QUART.q(1));

        // #25 (2 aliases: W.ore(ore_tungsten, ore_nether_tungsten))
        entry("ore_tungsten", MAT_TUNGSTEN, INGOT.q(2), MAT_STONE, QUART.q(1));
        entry("ore_nether_tungsten", MAT_TUNGSTEN, INGOT.q(2), MAT_STONE, QUART.q(1));

        // #26
        entry("ore_aluminium", MAT_ALUMINIUM, INGOT.q(2), MAT_SODIUM, NUGGET.q(3), MAT_STONE, QUART.q(1));

        // #28
        entry("ore_gneiss_gold", MAT_GOLD, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));

        // #29 (CE binds 7 aliases via U.ore(...); this port has 6 of the 7 items - the 7th,
        // ore_sellafield_uranium_scorched, is not registered anywhere in this port yet (a
        // structure/POI-associated variant - structure world-gen is unbuilt, per docs/phase6/
        // recipe_graph_audit.md). See "still blocked" below.
        entry("ore_uranium", MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        entry("ore_uranium_scorched", MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        entry("ore_gneiss_uranium", MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        entry("ore_gneiss_uranium_scorched", MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        entry("ore_nether_uranium", MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        entry("ore_nether_uranium_scorched", MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));

        // #30 (CE loops TH232.all(ORE) - 6 ore-dict alias *spellings* of the same single logical
        // entry/item; this port has one real backing item, ore_thorium).
        entry("ore_thorium", MAT_THORIUM, INGOT.q(2), MAT_URANIUM, NUGGET.q(3), MAT_STONE, QUART.q(1));

        // #31 (2 aliases: CU.ore(ore_copper, ore_gneiss_copper))
        entry("ore_copper", MAT_COPPER, INGOT.q(2), MAT_STONE, QUART.q(1));
        entry("ore_gneiss_copper", MAT_COPPER, INGOT.q(2), MAT_STONE, QUART.q(1));

        // #32
        entry("ore_lead", MAT_LEAD, INGOT.q(2), MAT_GOLD, NUGGET.q(1), MAT_STONE, QUART.q(1));

        // #33
        entry("ore_beryllium", MAT_BERYLLIUM, INGOT.q(2), MAT_STONE, QUART.q(1));

        // #34 (2 aliases: CO.ore(ore_cobalt, ore_nether_cobalt))
        entry("ore_cobalt", MAT_COBALT, INGOT.q(1), MAT_STONE, QUART.q(1));
        entry("ore_nether_cobalt", MAT_COBALT, INGOT.q(1), MAT_STONE, QUART.q(1));

        // ==================== GenericBlocks stone_resource_* (CE lines 125-129) ====================

        // #36
        entry("stone_resource_hematite", MAT_HEMATITE, INGOT.q(1));

        // #37
        entry("stone_resource_malachite", MAT_MALACHITE, INGOT.q(6));

        // #38: CE registers DictFrame.fromOne(stone_resource, LIMESTONE) (a metadata ItemStack);
        // this port's LIMESTONE variant of BlockResourceStone is its own distinct registered item.
        entry("stone_resource_limestone", MAT_FLUX, DUST.q(10));

        // ==================== BilletPowderItems (CE line 130) ====================

        // #39: notably both the MatDistribution source item *and* the shape POWDER_FLUX's own
        // generated-output family - melting flux powder gives back flux material 1:1, a CE quirk
        // (see MAT_FLUX's generator-class mapping in this task's research report).
        entry("powder_flux", MAT_FLUX, DUST.q(1));

        // ==================== still blocked (missing item/block registration elsewhere) ====================
        // None of these need new recipe logic - only a future item registration in the area named
        // below to reconnect. Yields are transcribed here (from CE MatDistribution.java) so
        // reconnecting is a one-line, no-re-derivation change once each item lands.

        // #8 / #9: CE ItemBase blades, registered by Phase11ProcessItems
        entry("blade_titanium", MAT_TITANIUM, INGOT.q(3));
        entry("blade_tungsten", MAT_TUNGSTEN, INGOT.q(3));

        // #17: registerEntry(ModItems.pipes_steel, MAT_STEEL, BLOCK.q(3))
        // TODO(machine_items or plumbing area): CE's hand-coded pipes_steel item is not registered
        // in this port. NOT the same item as MaterialItemGenerator's generic autogen "steel_pipe"
        // (MAT_STEEL's PIPE autogen shape) - those are two distinct CE items sharing only a
        // name-prefix; redirecting this entry to steel_pipe would change semantics, not just naming.

        // #18: registerEntry(DictFrame.fromOne(casing, SMALL), MAT_GUNMETAL, PLATE.q(1, 4))
        // #19: registerEntry(DictFrame.fromOne(casing, SMALL_STEEL), MAT_WEAPONSTEEL, PLATE.q(1, 4))
        // #20: registerEntry(DictFrame.fromOne(casing, LARGE), MAT_GUNMETAL, PLATE.q(1, 2))
        // #21: registerEntry(DictFrame.fromOne(casing, LARGE_STEEL), MAT_WEAPONSTEEL, PLATE.q(1, 2))
        // TODO(weapon/machine casing area): ModItems.casing / EnumCasingType - only the bare
        // EnumCasingType enum shell exists (com.hbm.items.ItemEnums.java), no real casing item.

        // #22: registerEntry(DictFrame.fromOne(chunk_ore, CRYOLITE), MAT_ALUMINIUM, INGOT.q(1), MAT_SODIUM, INGOT.q(1))
        // TODO(chunk_ore area): ModItems.chunk_ore / EnumChunkType.CRYOLITE - only the bare
        // EnumChunkType enum shell exists (com.hbm.items.ItemEnums.java); confirmed not registered
        // via BlockResourceStone.java/OreBlocks.java TODO comments citing chunk_ore as not-yet-registered.

        // #41: registerEntry(DictFrame.fromOne(powder_ash, WOOD), MAT_CARBON, NUGGET.q(1))
        // #42: registerEntry(DictFrame.fromOne(powder_ash, COAL), MAT_CARBON, NUGGET.q(2))
        // #43: registerEntry(DictFrame.fromOne(powder_ash, MISC), MAT_CARBON, NUGGET.q(1))
        // TODO(billet_powder area): ModItems.powder_ash / EnumAshType - deliberately excluded pending
        // an ItemEnumMulti flattening-naming decision, per BilletPowderItems.java's own javadoc.

        // #29 (partial): the 7th uranium-ore alias, ore_sellafield_uranium_scorched, is not
        // registered anywhere in this port - see the comment on the 6 registered uranium aliases
        // above.
    }

    /**
     * Resolves a mod item by its registry id string and registers its crucible yield, exactly the
     * resolution pattern {@code ModItemTagProvider.addTags()} already uses
     * ({@code BuiltInRegistries.ITEM.getOptional}). Every name passed in by {@link #registerAll()}
     * above is already confirmed present in this port's registry (see this class's own javadoc / the
     * research report's per-row "Ready?" column); {@link Mats#registerEntry(Item, Object...)} itself
     * performs no null-key check (a {@code null}-keyed map entry would simply never be looked up, not
     * throw), so this method guards defensively instead and logs rather than silently dropping the
     * entry if a name this class believes is real ever isn't - that would mean this class's data has
     * gone stale against the registry, worth surfacing.
     */
    private static void entry(String name, Object... matDef) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name)).orElse(null);
        if (item == null) {
            MainRegistry.logger.warn("MatDistributionDefaults: hbm:" + name + " is not registered - skipping its crucible entry (this class's data may be stale, see its javadoc)");
            return;
        }
        registerEntry(item, matDef);
    }
}
