package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreOutput;
import com.hbm.items.special.BedrockOreType;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CentrifugeRecipes} (the item-only "ore washing"
 * centrifuge - see {@code docs/phase2/machines_chemical_isotope.md}'s table distinguishing it from
 * the real isotope-separation "gas centrifuge", {@link GasCentrifugeRecipes}). A flat
 * {@code HashMap<AStack, ItemStack[]>} keyed by input, up to 4 outputs, exactly like CE.
 * <p>
 * <b>Scope trim</b> (documented, same shape as {@code RefineryRecipes}'s own precedent): CE registers
 * ~50 recipes, many keyed against items/blocks this port has not registered yet
 * ({@code ItemBedrockOreNew}, {@code chunk_ore}, several rare-earth/schrabidium ores) or against
 * 1.12 OreDictionary string names with no confirmed modern tag equivalent in this port yet. This
 * class ports a representative real subset - the common-ore washing recipes plus the crystal-to-powder
 * breakdown recipes - using only items already confirmed present in this port
 * ({@link BilletPowderItems}/{@link IngotNuggetItems}/{@link PlateCrystalWasteItems}), preserving
 * CE's exact output quantities for every recipe it does carry. Vanilla ore blocks are matched via
 * NeoForge's common {@code c:ores/*} tags ({@link OreDictStack#ofCommonTag}) rather than CE's 1.12
 * OreDictionary strings, per {@code RecipesCommon}'s own documented tag-based replacement.
 */
public final class CentrifugeRecipes {

    public static final Map<AStack, ItemStack[]> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CentrifugeRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // ore washing: 1 ore -> 3x powder + 1 vanilla gravel byproduct, exactly CE's shape/quantities
        RECIPES.put(OreDictStack.ofCommonTag("ores/iron"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/gold"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/copper"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/lead"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/diamond"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/emerald"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/uranium"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_RA226.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/lapis"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LAPIS.get(), 6),
                new ItemStack(BilletPowderItems.POWDER_COBALT_TINY.get(), 1),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/redstone"), new ItemStack[]{
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/coal"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 2),
                new ItemStack(Items.GRAVEL, 1)});

        // crystal breakdown, exact CE quantities
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COAL.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_IRON.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_GOLD.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_REDSTONE.get()), new ItemStack[]{
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.REDSTONE, 3)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LAPIS.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LAPIS.get(), 4),
                new ItemStack(BilletPowderItems.POWDER_LAPIS.get(), 4),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 1),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 2)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_DIAMOND.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_URANIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_RA226.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COPPER.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 2),
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get()),
                new ItemStack(BilletPowderItems.POWDER_COBALT_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LEAD.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // CE CentrifugeRecipes.java:271 — was a 1-output stub; full 4-out now that niter exists
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get()), new ItemStack[]{
                new ItemStack(item("niter"), 3),
                new ItemStack(item("niter"), 3),
                new ItemStack(item("niter"), 3),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // CE CentrifugeRecipes.java:267-285 remaining crystals whose I/O is registered
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_THORIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_RA226.get(), 1)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_PLUTONIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_POLONIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TITANIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TUNGSTEN.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_BERYLLIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRARANIUM.get()), new ItemStack[]{
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 2)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRABIDIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_RARE.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DESH_MIX.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DESH_MIX.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get(), 2)});
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COBALT.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // CE CentrifugeRecipes.java:59 / :89 / :95 / :101 / :125 / :131 / :149 / :155 / :161 / :173
        // / :185 / :191 / :197 / :203 / :256 / :258 / :270 / :275 / :281-284
        RECIPES.put(OreDictStack.ofCommonTag("ores/lignite"), stacks(
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_lignite")), stacks(
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(OreDictStack.ofCommonTag("ores/titanium"), stacks(
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()),
                new ItemStack(BilletPowderItems.POWDER_IRON.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_titanium")), stacks(
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()),
                new ItemStack(BilletPowderItems.POWDER_IRON.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(OreDictStack.ofCommonTag("ores/quartz"), stacks(
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get()),
                new ItemStack(Items.NETHERRACK)));
        RECIPES.put(new ComparableStack(block("ore_tungsten")), stacks(
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get()),
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get()),
                new ItemStack(BilletPowderItems.POWDER_IRON.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_schrabidium")), stacks(
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get()),
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get()),
                new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_rare")), stacks(
                new ItemStack(BilletPowderItems.POWDER_DESH_MIX.get()),
                new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get(), 2),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_thorium")), stacks(
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_beryllium")), stacks(
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_fluorite")), stacks(
                new ItemStack(item("fluorite"), 3),
                new ItemStack(item("fluorite"), 3),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(block("ore_tikite")), stacks(
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get()),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get(), 2),
                new ItemStack(Items.END_STONE)));
        RECIPES.put(new ComparableStack(block("block_euphemium_cluster")), stacks(
                new ItemStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get(), 7),
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 4),
                new ItemStack(IngotNuggetItems.INGOT_STARMETAL.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 2)));
        RECIPES.put(new ComparableStack(block("ore_nether_fire")), stacks(
                new ItemStack(Items.BLAZE_POWDER, 2),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 2),
                new ItemStack(IngotNuggetItems.INGOT_PHOSPHORUS.get()),
                new ItemStack(Items.NETHERRACK)));
        RECIPES.put(new ComparableStack(block("ore_cobalt")), stacks(
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get()),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get()),
                new ItemStack(Items.GRAVEL)));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_TEKTITE.get()), stacks(
                new ItemStack(BilletPowderItems.POWDER_METEORITE_TINY.get()),
                new ItemStack(BilletPowderItems.POWDER_PALEOGENITE_TINY.get()),
                new ItemStack(BilletPowderItems.POWDER_METEORITE_TINY.get()),
                new ItemStack(item("dust"), 6)));
        RECIPES.put(new ComparableStack(Items.BLAZE_ROD), stacks(
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get()),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get())));
        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_SCHRARANIUM.get()), stacks(
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get()),
                new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get(), 3),
                new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get()), stacks(
                new ItemStack(item("sulfur"), 4),
                new ItemStack(item("sulfur"), 4),
                new ItemStack(BilletPowderItems.POWDER_IRON.get()),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get())));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get()), stacks(
                new ItemStack(item("fluorite"), 4),
                new ItemStack(item("fluorite"), 4),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get())));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_PHOSPHORUS.get()), stacks(
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 3),
                new ItemStack(IngotNuggetItems.INGOT_PHOSPHORUS.get(), 2),
                new ItemStack(Items.BLAZE_POWDER, 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get()), stacks(
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_NITAN_MIX.get())));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LITHIUM.get()), stacks(
                new ItemStack(BilletPowderItems.POWDER_LITHIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()),
                new ItemStack(item("fluorite"))));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_STARMETAL.get()), stacks(
                new ItemStack(BilletPowderItems.POWDER_DURA_STEEL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_ASTATINE.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 5)));

        // CE CentrifugeRecipes.java:220-241 — 16 templates × 6 BedrockOreType
        for (BedrockOreType type : BedrockOreType.VALUES) {
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.BASE)), stacks(
                    bedrock(type, BedrockOreGrade.PRIMARY, 1), new ItemStack(Items.GRAVEL)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.BASE_ROASTED)), stacks(
                    bedrock(type, BedrockOreGrade.PRIMARY, 1), new ItemStack(Items.GRAVEL)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.BASE_WASHED)), stacks(
                    bedrock(type, BedrockOreGrade.PRIMARY, 1),
                    bedrock(type, BedrockOreGrade.PRIMARY, 1),
                    new ItemStack(Items.GRAVEL)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_SULFURIC)), stacks(
                    bedrock(type, BedrockOreGrade.PRIMARY_NOSULFURIC, 2),
                    bedrock(type, BedrockOreGrade.SULFURIC_BYPRODUCT, 2)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_SOLVENT)), stacks(
                    bedrock(type, BedrockOreGrade.PRIMARY_NOSOLVENT, 2),
                    bedrock(type, BedrockOreGrade.SULFURIC_BYPRODUCT, 2),
                    bedrock(type, BedrockOreGrade.SOLVENT_BYPRODUCT, 2)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_RAD)), stacks(
                    bedrock(type, BedrockOreGrade.PRIMARY_NORAD, 2),
                    bedrock(type, BedrockOreGrade.SULFURIC_BYPRODUCT, 2),
                    bedrock(type, BedrockOreGrade.SOLVENT_BYPRODUCT, 2),
                    bedrock(type, BedrockOreGrade.RAD_BYPRODUCT, 2)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY)), stacks(
                    extract(type.primary1), extract(type.primary2)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_ROASTED)), stacks(
                    extract(type.primary1), extract(type.primary2)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_NOSULFURIC)), stacks(
                    extract(type.primary1), extract(type.primary2),
                    bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_NOSOLVENT)), stacks(
                    extract(type.primary1), extract(type.primary2),
                    bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_NORAD)), stacks(
                    extract(type.primary1), extract(type.primary2),
                    bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_FIRST)), stacks(
                    extract(type.primary1), extract(type.primary1),
                    extract(type.primary2), bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY_SECOND)), stacks(
                    extract(type.primary1), extract(type.primary2),
                    extract(type.primary2), bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.SULFURIC_WASHED)), stacks(
                    extract(type.byproductAcid1), extract(type.byproductAcid2),
                    extract(type.byproductAcid3), bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.SOLVENT_WASHED)), stacks(
                    extract(type.byproductSolvent1), extract(type.byproductSolvent2),
                    extract(type.byproductSolvent3), bedrock(type, BedrockOreGrade.CRUMBS, 1)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.RAD_WASHED)), stacks(
                    extract(type.byproductRad1), extract(type.byproductRad2),
                    extract(type.byproductRad3), bedrock(type, BedrockOreGrade.CRUMBS, 1)));
        }

        RECIPES.entrySet().removeIf(e -> {
            for (ItemStack s : e.getValue()) {
                if (s == null || s.isEmpty() || s.getItem() == Items.AIR) return true;
            }
            return false;
        });
    }

    private static ItemStack[] stacks(ItemStack... out) {
        return out;
    }

    private static ItemStack bedrock(BedrockOreType type, BedrockOreGrade grade) {
        return bedrock(type, grade, 1);
    }

    private static ItemStack bedrock(BedrockOreType type, BedrockOreGrade grade, int n) {
        return new ItemStack(BedrockOreItems.get(type, grade).get(), n);
    }

    /** CE {@code ItemBedrockOreNew.extract} — flattened to {@code <mat>_ore_fragment}. */
    private static ItemStack extract(BedrockOreOutput output) {
        int count = Math.min((int) Math.ceil(output.amount()), 64);
        String id = MaterialShapes.FRAGMENT.buildRegistryName(output.material());
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
        return new ItemStack(item, count);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    /**
     * Ported from CE's {@code CentrifugeRecipes.getOutput}: exact match first, then a linear scan
     * for the first applicable {@link AStack} (tag membership) - identical lookup order to CE.
     */
    public static ItemStack[] getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        for (Map.Entry<AStack, ItemStack[]> entry : RECIPES.entrySet()) {
            if (entry.getKey().matchesRecipe(stack, true)) {
                ItemStack[] out = entry.getValue();
                ItemStack[] copy = new ItemStack[out.length];
                for (int i = 0; i < out.length; i++) copy[i] = out[i].copy();
                return copy;
            }
        }
        return null;
    }
}
