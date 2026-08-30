package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.util.Tuple.Pair;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CyclotronRecipes} - particle-accelerator
 * transmutation: a catalyst item ("particle") + a target {@link AStack} together produce one output
 * item plus an antimatter mB yield ({@code docs/phase2/machines_chemical_isotope.md}'s Cyclotron
 * section).
 * <p>
 * <b>Item substitution</b> (documented): CE's catalyst items ({@code part_lithium},
 * {@code part_beryllium}, {@code part_carbon}, {@code part_copper}, {@code part_plutonium} -
 * dedicated "atom smasher particle" items) are not registered in this port yet. This class
 * substitutes the corresponding elemental powder from {@link BilletPowderItems} (e.g.
 * {@code powder_lithium} for {@code part_lithium}) as the catalyst item, keeping every
 * target/output/antimatter-yield number from CE exactly - <b>TODO(items-followup)</b>: swap in the
 * real {@code part_*} items once that items area registers them. Target ores are matched via
 * NeoForge's common {@code c:dusts/*} tags in place of CE's 1.12 OreDictionary {@code dust*} strings.
 */
public final class CyclotronRecipes {

    public static final Map<Pair<ComparableStack, AStack>, Pair<ItemStack, Integer>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CyclotronRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // lithium catalyst chain (part_lithium -> powder_lithium substitute), amat yield 50 each - exact CE values
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/beryllium"), new ItemStack(BilletPowderItems.POWDER_BORON.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/boron"), new ItemStack(BilletPowderItems.POWDER_COAL.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/iron"), new ItemStack(BilletPowderItems.POWDER_COBALT.get()), 50);
        makeRecipe(BilletPowderItems.POWDER_LITHIUM, OreDictStack.ofCommonTag("dusts/gold"), new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 50);

        // beryllium catalyst chain (part_beryllium -> powder_beryllium substitute)
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, OreDictStack.ofCommonTag("dusts/lithium"), new ItemStack(BilletPowderItems.POWDER_BORON.get()), 25);
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, OreDictStack.ofCommonTag("dusts/titanium"), new ItemStack(BilletPowderItems.POWDER_IRON.get()), 25);
        makeRecipe(BilletPowderItems.POWDER_BERYLLIUM, OreDictStack.ofCommonTag("dusts/cobalt"), new ItemStack(BilletPowderItems.POWDER_COPPER.get()), 25);

        // copper catalyst chain (part_copper -> powder_copper substitute)
        makeRecipe(BilletPowderItems.POWDER_COPPER, OreDictStack.ofCommonTag("dusts/beryllium"), new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, OreDictStack.ofCommonTag("dusts/iron"), new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), 15);
        makeRecipe(BilletPowderItems.POWDER_COPPER, OreDictStack.ofCommonTag("dusts/gold"), new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), 15);

        // plutonium catalyst chain (part_plutonium -> powder_plutonium substitute) - large amat yield, exact CE value
        makeRecipe(BilletPowderItems.POWDER_PLUTONIUM, OreDictStack.ofCommonTag("dusts/plutonium"), new ItemStack(BilletPowderItems.POWDER_TENNESSINE.get()), 100);
    }

    private static void makeRecipe(net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> part, AStack in, ItemStack out, int amat) {
        RECIPES.put(new Pair<>(new ComparableStack(part.get()), in), new Pair<>(out, amat));
    }

    /**
     * Ported from CE's {@code CyclotronRecipes.getOutput}: linear scan for the first recipe whose
     * catalyst and target both match, returning {@code {output ItemStack, antimatter mB Integer}}.
     */
    public static Object[] getOutput(ItemStack target, ItemStack catalyst) {
        if (target == null || target.isEmpty() || catalyst == null || catalyst.isEmpty()) return null;

        ComparableStack catalystKey = new ComparableStack(catalyst).makeSingular();

        for (Entry<Pair<ComparableStack, AStack>, Pair<ItemStack, Integer>> entry : RECIPES.entrySet()) {
            if (entry.getKey().getKey().isApplicable(catalystKey.getStack()) && entry.getKey().getValue().isApplicable(target)) {
                return new Object[]{entry.getValue().getKey().copy(), entry.getValue().getValue()};
            }
        }
        return null;
    }
}
