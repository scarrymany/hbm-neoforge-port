package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});
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
