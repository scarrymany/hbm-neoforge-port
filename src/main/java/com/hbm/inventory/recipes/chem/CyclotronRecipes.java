package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
 * Catalysts are CE {@code part_*} ({@link Phase11ProcessItems}). Target {@code dust*} OreDict
 * strings are {@code c:dusts/*}. CE field {@code ingot_mercury} is registry id {@code nugget_mercury}
 * — do not invent {@code ingot_mercury}. {@code dustPhosphorus} has no port common-tag member;
 * the live HBM row is {@code powder_fire} (CE phosphorus powder).
 */
public final class CyclotronRecipes {

    public static final Map<Pair<ComparableStack, AStack>, Pair<ItemStack, Integer>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CyclotronRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE CyclotronRecipes.java:30-46 lithium
        int liA = 50;
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/lithium"), new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/beryllium"), new ItemStack(BilletPowderItems.POWDER_BORON.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/boron"), new ItemStack(BilletPowderItems.POWDER_COAL.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/quartz"), new ItemStack(BilletPowderItems.POWDER_FIRE.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_FIRE.get()), new ItemStack(item("sulfur")), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/iron"), new ItemStack(BilletPowderItems.POWDER_COBALT.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_STRONTIUM.get()), new ItemStack(BilletPowderItems.POWDER_ZIRCONIUM.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/gold"), new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_POLONIUM.get()), new ItemStack(BilletPowderItems.POWDER_ASTATINE.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_LANTHANIUM.get()), new ItemStack(BilletPowderItems.POWDER_CERIUM.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_ACTINIUM.get()), new ItemStack(BilletPowderItems.POWDER_THORIUM.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, OreDictStack.ofCommonTag("dusts/uranium"), new ItemStack(BilletPowderItems.POWDER_NEPTUNIUM.get()), liA);
        makeRecipe(Phase11ProcessItems.PART_LITHIUM, new ComparableStack(BilletPowderItems.POWDER_NEPTUNIUM.get()), new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get()), liA);

        // CE CyclotronRecipes.java:48-58 beryllium
        int beA = 25;
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, OreDictStack.ofCommonTag("dusts/lithium"), new ItemStack(BilletPowderItems.POWDER_BORON.get()), beA);
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, OreDictStack.ofCommonTag("dusts/quartz"), new ItemStack(item("sulfur")), beA);
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, OreDictStack.ofCommonTag("dusts/titanium"), new ItemStack(BilletPowderItems.POWDER_IRON.get()), beA);
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, OreDictStack.ofCommonTag("dusts/cobalt"), new ItemStack(BilletPowderItems.POWDER_COPPER.get()), beA);
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, new ComparableStack(BilletPowderItems.POWDER_STRONTIUM.get()), new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), beA);
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, new ComparableStack(BilletPowderItems.POWDER_CERIUM.get()), new ItemStack(BilletPowderItems.POWDER_NEODYMIUM.get()), beA);
        makeRecipe(Phase11ProcessItems.PART_BERYLLIUM, new ComparableStack(BilletPowderItems.POWDER_THORIUM.get()), new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), beA);

        // CE CyclotronRecipes.java:60-71 carbon
        int caA = 10;
        makeRecipe(Phase11ProcessItems.PART_CARBON, OreDictStack.ofCommonTag("dusts/boron"), new ItemStack(BilletPowderItems.POWDER_ALUMINIUM.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, new ComparableStack(item("sulfur")), new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, OreDictStack.ofCommonTag("dusts/titanium"), new ItemStack(BilletPowderItems.POWDER_COBALT.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, new ComparableStack(BilletPowderItems.POWDER_CAESIUM.get()), new ItemStack(BilletPowderItems.POWDER_LANTHANIUM.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, new ComparableStack(BilletPowderItems.POWDER_NEODYMIUM.get()), new ItemStack(BilletPowderItems.POWDER_GOLD.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, new ComparableStack(IngotNuggetItems.NUGGET_MERCURY.get()), new ItemStack(BilletPowderItems.POWDER_POLONIUM.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, OreDictStack.ofCommonTag("dusts/lead"), new ItemStack(BilletPowderItems.POWDER_RA226.get()), caA);
        makeRecipe(Phase11ProcessItems.PART_CARBON, new ComparableStack(BilletPowderItems.POWDER_ASTATINE.get()), new ItemStack(BilletPowderItems.POWDER_ACTINIUM.get()), caA);

        // CE CyclotronRecipes.java:73-86 copper
        int coA = 15;
        makeRecipe(Phase11ProcessItems.PART_COPPER, OreDictStack.ofCommonTag("dusts/beryllium"), new ItemStack(BilletPowderItems.POWDER_QUARTZ.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, OreDictStack.ofCommonTag("dusts/coal"), new ItemStack(BilletPowderItems.POWDER_BROMINE.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, OreDictStack.ofCommonTag("dusts/titanium"), new ItemStack(BilletPowderItems.POWDER_STRONTIUM.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, OreDictStack.ofCommonTag("dusts/iron"), new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, new ComparableStack(BilletPowderItems.POWDER_BROMINE.get()), new ItemStack(BilletPowderItems.POWDER_IODINE.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, new ComparableStack(BilletPowderItems.POWDER_STRONTIUM.get()), new ItemStack(BilletPowderItems.POWDER_NEODYMIUM.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, new ComparableStack(BilletPowderItems.POWDER_NIOBIUM.get()), new ItemStack(BilletPowderItems.POWDER_CAESIUM.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, new ComparableStack(BilletPowderItems.POWDER_IODINE.get()), new ItemStack(BilletPowderItems.POWDER_POLONIUM.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, new ComparableStack(BilletPowderItems.POWDER_CAESIUM.get()), new ItemStack(BilletPowderItems.POWDER_ACTINIUM.get()), coA);
        makeRecipe(Phase11ProcessItems.PART_COPPER, OreDictStack.ofCommonTag("dusts/gold"), new ItemStack(BilletPowderItems.POWDER_URANIUM.get()), coA);

        // CE CyclotronRecipes.java:88-95 plutonium
        int plA = 100;
        makeRecipe(Phase11ProcessItems.PART_PLUTONIUM, new ComparableStack(BilletPowderItems.POWDER_FIRE.get()), new ItemStack(BilletPowderItems.POWDER_TENNESSINE.get()), plA);
        makeRecipe(Phase11ProcessItems.PART_PLUTONIUM, OreDictStack.ofCommonTag("dusts/plutonium"), new ItemStack(BilletPowderItems.POWDER_TENNESSINE.get()), plA);
        makeRecipe(Phase11ProcessItems.PART_PLUTONIUM, new ComparableStack(BilletPowderItems.POWDER_TENNESSINE.get()), new ItemStack(BilletPowderItems.POWDER_AUSTRALIUM.get()), plA);
        makeRecipe(Phase11ProcessItems.PART_PLUTONIUM, new ComparableStack(Phase11ProcessItems.PELLET_CHARGED.get()), new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get()), 1000);
    }

    private static void makeRecipe(net.neoforged.neoforge.registries.DeferredItem<Item> part, AStack in, ItemStack out, int amat) {
        RECIPES.put(new Pair<>(new ComparableStack(part.get()), in), new Pair<>(out, amat));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
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
