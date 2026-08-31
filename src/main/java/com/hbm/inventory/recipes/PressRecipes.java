package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.machine.ItemStamp;
import com.hbm.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.PressRecipes} (153 lines, read in full; see
 * {@code docs/phase7/mrec_14_annihilator_misc.md}) - the Press machine's recipe table, keyed by a
 * two-part match: an ingredient ({@link AStack}) and which {@link ItemStamp.StampType} the
 * player's inserted stamp resolves to. CE's own {@code getOutput(ItemStack, ItemStack)} is a
 * linear scan over stamp-type equality + {@code AStack.matchesRecipe(...)}, not a direct map
 * lookup (because {@code AStack} equality is not a structural/semantic match) - reproduced
 * verbatim in {@link #getOutput(ItemStack, ItemStack)} below.
 *
 * <p><b>Shape decision (research report's "Recommended implementation shape", option 1 of 2)</b>:
 * kept as a plain hardcoded {@link Map}, following {@link RefineryRecipes}/{@link FractionRecipes}'s
 * established non-JSON precedent, rather than a new {@code Recipe<?>}/{@code RecipeSerializer}
 * family (the report's option 2). The report frames option 2 as the "if bandwidth allows" choice
 * for a future pass completing the <i>whole</i> file (once {@code wire_fine}/{@code casing_*}/
 * {@code circuit_*}/{@code page_of_*} land) - since <b>no Press machine/block-entity exists in this
 * port yet</b> (confirmed by the research report) to consume a JSON-drivable recipe today, and only
 * a fraction of CE's ~48 entries are currently item-ready (see "Scope trim" below), the lower-risk,
 * lower-effort option 1 is used here; nothing below blocks a future migration to option 2.
 *
 * <p><b>Scope trim vs. CE (documented, not silent)</b>: CE registers ≈48 entries across 6 groups
 * (FLAT, PLATE, casings, WIRE, CIRCUIT, PRINTING). This class ports <b>17</b> - the full 13-entry
 * PLATE group, plus the 4 FLAT-group entries whose dust ingredient and gem output are both
 * confirmed registered ({@code netherquartz}/{@code lapis}/{@code diamond}/{@code emerald}). The
 * remaining ≈31 entries are <b>not</b> ported - every one needs at least one item this port has not
 * registered under any name:
 * <ul>
 *     <li><b>FLAT</b> (7 of 11 not ported): {@code ModItems.biomass}/{@code biomass_compressed}
 *     (confirmed absent - independently corroborated by {@code PyroOvenRecipes}'s and
 *     {@code SolidificationRecipes}'s own javadoc, which separately name this exact gap);
 *     {@code ANY_COKE.gem()} (CE's ore-dict wildcard over coal-coke/pet-coke/lignite-coke has no
 *     port-side umbrella-tag equivalent, and no concrete coke item is registered under any name
 *     either, so there is no substitute concrete ingredient); {@code meteorite_sword_reforged}/
 *     {@code _hardened} (neither registered - the meteorite sword tier family is deferred, per
 *     {@code WeaponMeleeItems}' own javadoc); {@code Blocks.LOG:3} &rarr; {@code ball_resin}
 *     ({@code ball_resin} not registered); coal/lignite dust &rarr; {@code briquette(COAL/LIGNITE)}
 *     and {@code powder_sawdust} &rarr; {@code briquette(WOOD)} (all 3 blocked purely on the output
 *     side - {@code EnumBriquetteType} exists as a bare enum in {@code ItemEnums} but zero
 *     {@code briquette_*} items are registered against it anywhere in this port, confirmed by grep).</li>
 *     <li><b>Casings</b> (4 of 4): {@code EnumCasingType} exists as a bare enum but zero
 *     {@code casing_*} items are registered against it.</li>
 *     <li><b>WIRE</b> (≈11 of ≈11, the {@code Mats.orderedList} loop): {@code wire_fine} is not
 *     registered under any name anywhere in this port (confirmed by grep - multiple independent
 *     TODOs elsewhere already flag this).</li>
 *     <li><b>CIRCUIT</b> (1 of 1): {@code EnumCircuitType} exists as a bare enum but zero
 *     {@code circuit_*} items are registered against it.</li>
 *     <li><b>PRINTING</b> (8 of 8): {@code EnumPages} exists as a bare enum but zero
 *     {@code page_of_*} items are registered against it.</li>
 * </ul>
 * None of these gaps are guessed at or stubbed here - see the coordinator's still-blocked list for
 * the exact per-entry citation.
 *
 * <p><b>Correction vs. the research report</b>: the report estimated only ~12-13 ready entries (the
 * PLATE group minus {@code plate_combine_steel}, whose {@code MAT_CMB}/ingredient status it flagged
 * as unconfirmed) and separately flagged the FLAT group's dust ingredients
 * ({@code netherquartz}/{@code lapis}/{@code diamond}) as blocked because no {@code Mats.MAT_QUARTZ}/
 * {@code MAT_LAPIS} constant exists and {@code MAT_DIAMOND} has no {@code DUST} in its autogen list.
 * Both undercount what is actually registered: (1) {@code Mats.MAT_CMB} <i>does</i> exist (Mats.java
 * line 169) and {@code ingot_combine_steel}/{@code plate_combine_steel} are both real registered
 * items, so all 13 PLATE entries are ready, not 12; (2) {@link BilletPowderItems} hand-registers
 * {@code powder_quartz}/{@code powder_lapis}/{@code powder_diamond}/{@code powder_emerald} directly
 * (CE's "powder" = this port's DUST shape), entirely independent of whether {@link com.hbm.inventory.material.Mats}
 * has a matching autogen-driven material constant - the report's dependency check, which searched
 * for {@code Mats.MAT_*} as its proxy for "does this dust exist", missed this hand-curated legacy
 * item family the same way {@code CrystallizerRecipes}'s own header independently documents for
 * {@code powder_diamond}. Both corrections only add ready entries; nothing the report marked ready
 * turned out to be blocked.
 *
 * <p><b>Ingredient matching (deliberate, documented deviation from the {@link RefineryRecipes}/
 * {@link com.hbm.inventory.recipes.ArcWelderRecipes} tag-based idiom)</b>: every ingredient below
 * is a concrete-item {@link ComparableStack}, not an {@code OreDictStack} against
 * {@code MaterialShapes.INGOT.commonTag(mat)} / {@code MaterialShapes.DUST.commonTag(mat)}. This
 * matches the precedent {@link CrystallizerRecipes}/{@link MixerRecipes}/{@link MagicRecipes}
 * already established for the identical situation (see {@code MagicRecipes}'s own header): this
 * port's {@code ModItemTagProvider#addLegacyMaterialTags()} tags a legacy {@code ingot_}/
 * {@code powder_}-family item under its material's common tag only when the material's first CE
 * alias, lowercased, textually equals the item's own id suffix - true for most of the 13 PLATE
 * materials (e.g. {@code Titanium} &rarr; {@code ingot_titanium}) but independently re-verified
 * <b>false</b> for 3 of them this task found by direct inspection: {@code Aluminum} (alias) vs.
 * {@code ingot_aluminium} (real id, British spelling), {@code CMBSteel} (alias) vs.
 * {@code ingot_combine_steel} (real id), {@code DuraSteel} (alias) vs. {@code ingot_dura_steel}
 * (real id, underscored). Rather than special-case only those 3, every PLATE/FLAT ingredient here
 * uses the same concrete-item match, avoiding the need to separately verify tag coverage per
 * material (unconfirmed tag coverage is a real, already-documented risk elsewhere in this exact
 * codebase, not a one-off concern invented for this file).
 *
 * <p><b>Not yet built: the Press block/block-entity itself</b> (confirmed absent by the research
 * report - {@code MachinePress}/{@code TileEntityMachinePress} and all GUI/container/render classes
 * are zero in this port). This class is recipe data only, ready for whichever future pass builds
 * that machine to consume via {@link #getOutput(ItemStack, ItemStack)}.
 */
public final class PressRecipes {

    private static final Map<Tuple.Pair<AStack, ItemStamp.StampType>, ItemStack> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private PressRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // ---- FLAT group (CE PressRecipes.java lines ~58-65, partial - see class javadoc "Scope trim") ----
        makeRecipe(ItemStamp.StampType.FLAT, new ComparableStack(BilletPowderItems.POWDER_QUARTZ.get()), Items.QUARTZ);
        // CE: new ItemStack(Items.DYE, 1, 4) (vanilla lapis dye metadata). Modern MC has no dye
        // metadata - Items.LAPIS_LAZULI is the direct, non-meta successor item.
        makeRecipe(ItemStamp.StampType.FLAT, new ComparableStack(BilletPowderItems.POWDER_LAPIS.get()), Items.LAPIS_LAZULI);
        makeRecipe(ItemStamp.StampType.FLAT, new ComparableStack(BilletPowderItems.POWDER_DIAMOND.get()), Items.DIAMOND);
        makeRecipe(ItemStamp.StampType.FLAT, new ComparableStack(BilletPowderItems.POWDER_EMERALD.get()), Items.EMERALD);

        // ---- PLATE group (CE PressRecipes.java lines ~74-86): ingot -> plate, all 13 entries ----
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(Items.IRON_INGOT), PlateCrystalWasteItems.PLATE_IRON.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(Items.GOLD_INGOT), PlateCrystalWasteItems.PLATE_GOLD.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_TITANIUM.get()), PlateCrystalWasteItems.PLATE_TITANIUM.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_ALUMINIUM.get()), PlateCrystalWasteItems.PLATE_ALUMINIUM.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_STEEL.get()), PlateCrystalWasteItems.PLATE_STEEL.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_LEAD.get()), PlateCrystalWasteItems.PLATE_LEAD.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_COPPER.get()), PlateCrystalWasteItems.PLATE_COPPER.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_SCHRABIDIUM.get()), PlateCrystalWasteItems.PLATE_SCHRABIDIUM.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_COMBINE_STEEL.get()), PlateCrystalWasteItems.PLATE_COMBINE_STEEL.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_GUNMETAL.get()), PlateCrystalWasteItems.PLATE_GUNMETAL.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_WEAPONSTEEL.get()), PlateCrystalWasteItems.PLATE_WEAPONSTEEL.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_SATURNITE.get()), PlateCrystalWasteItems.PLATE_SATURNITE.get());
        makeRecipe(ItemStamp.StampType.PLATE, new ComparableStack(IngotNuggetItems.INGOT_DURA_STEEL.get()), PlateCrystalWasteItems.PLATE_DURA_STEEL.get());

        // Deliberately not ported (see class javadoc "Scope trim"): the other 7 FLAT entries
        // (biomass, ANY_COKE.gem(), meteorite_sword_*, ball_resin, 3x briquette), all 4 casing
        // entries, the ~11-entry WIRE loop, the CIRCUIT entry, and all 8 PRINTING entries - every
        // one needs at least one item this port has not registered under any name.
    }

    private static void makeRecipe(ItemStamp.StampType type, AStack input, Item output) {
        RECIPES.put(new Tuple.Pair<>(input, type), new ItemStack(output));
    }

    /**
     * Verbatim port of CE's {@code PressRecipes.getOutput(ItemStack, ItemStack)}: a linear scan
     * matching stamp-type equality plus {@code AStack.matchesRecipe(...)}, not a direct map lookup
     * (mirrored here for the same reason CE itself is not a direct lookup - {@link AStack} equality
     * is not the same relation as {@link AStack#matchesRecipe}).
     */
    public static ItemStack getOutput(ItemStack ingredient, ItemStack stamp) {
        register();

        if (ingredient == null || ingredient.isEmpty() || stamp == null || stamp.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!(stamp.getItem() instanceof ItemStamp itemStamp)) {
            return ItemStack.EMPTY;
        }
        ItemStamp.StampType type = itemStamp.getStampType();

        for (Map.Entry<Tuple.Pair<AStack, ItemStamp.StampType>, ItemStack> recipe : RECIPES.entrySet()) {
            if (recipe.getKey().getValue() == type && recipe.getKey().getKey().matchesRecipe(ingredient, true)) {
                return recipe.getValue();
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Full-collection accessor for a future JEI category / Press block entity, matching
     * {@link RefineryRecipes#getAllRefinery()}'s established pattern (defensive lazy registration,
     * unmodifiable view).
     */
    public static Map<Tuple.Pair<AStack, ItemStamp.StampType>, ItemStack> getAllRecipes() {
        register();
        return Collections.unmodifiableMap(RECIPES);
    }
}
