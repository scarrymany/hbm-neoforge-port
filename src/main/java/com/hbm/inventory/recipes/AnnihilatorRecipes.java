package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.MachineItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code AnnihilatorRecipes.java}:60-77. Milestone table (CE gates on 528; this port
 * registers the same {@code recipes.put} sites unconditionally so the machine is usable).
 */
public final class AnnihilatorRecipes {

    public static final HashMap<Object, AnnihilatorRecipe> recipes = new HashMap<>();

    private static boolean registered = false;

    private AnnihilatorRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        registerDefaults();
    }

    public static void registerDefaults() {
        // CE AnnihilatorRecipes.java:61-76 — each recipes.put is a census site
        recipes.put(new ComparableStack(item("ingot_steel")), milestone(256, "steel"));
        recipes.put(new ComparableStack(item("billet_silicon")), milestone(256, "chip"));
        recipes.put(new ComparableStack(item("nugget_bismuth")), milestone(128, "chip_bismoid"));
        recipes.put(new ComparableStack(item("pellet_charged")), milestone(1024, "chip_quantum"));

        recipes.put(new ComparableStack(item("billet_u238")), milestone(256, "gascent"));
        recipes.put(new ComparableStack(item("ingot_polymer")), milestone(512, "plastic"));
        recipes.put(new ComparableStack(item("ingot_rubber")), milestone(512, "rubber"));
        recipes.put(new ComparableStack(item("ingot_ferrouranium")), milestone(1024, "ferrouranium"));
        recipes.put(new ComparableStack(item("powder_strontium")), milestone(256, "strontium"));
        recipes.put(new ComparableStack(item("ingot_bakelite")), milestone(1024, "hardplastic"));
        recipes.put(new ComparableStack(item("ingot_tcalloy")), milestone(1024, "tcalloy"));
        recipes.put(new ComparableStack(item("powder_chlorophyte")), milestone(1024, "chlorophyte"));

        recipes.put(new ComparableStack(item("ammo_standard_bmg50_fmj")), milestone(256, "bmg"));
        recipes.put(new ComparableStack(item("ammo_arty")), milestone(128, "arty"));
        recipes.put(new ComparableStack(item("circuit_controller")), milestone(128, "controller"));
    }

    public static AnnihilatorRecipe get(Object key) {
        register();
        return recipes.get(key);
    }

    public static ItemStack getHighestPayoutFromKey(Object key, BigInteger prevAmount, BigInteger currentAmount) {
        register();
        AnnihilatorRecipe recipe = recipes.get(key);
        if (recipe != null) return getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
        return ItemStack.EMPTY;
    }

    public static ItemStack getHighestPayoutFromStack(ItemStack stack, BigInteger prevAmount, BigInteger currentAmount) {
        register();
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack fromItem = getHighestPayoutFromKey(stack.getItem(), prevAmount, currentAmount);
        if (!fromItem.isEmpty()) return fromItem;
        return getHighestPayoutFromKey(new ComparableStack(stack.getItem()), prevAmount, currentAmount);
    }

    public static ItemStack getHighestPayoutFromRecipe(AnnihilatorRecipe recipe, BigInteger prevAmount, BigInteger currentAmount) {
        BigInteger highestYet = BigInteger.ZERO;
        ItemStack highestPayout = ItemStack.EMPTY;
        for (Milestone milestone : recipe.milestones) {
            if (prevAmount != null && prevAmount.compareTo(milestone.amount) >= 0) continue;
            if (currentAmount.compareTo(highestYet) <= 0) continue;
            if (currentAmount.compareTo(milestone.amount) >= 0) {
                highestYet = milestone.amount;
                highestPayout = milestone.payout;
            }
        }
        return highestPayout.isEmpty() ? ItemStack.EMPTY : highestPayout.copy();
    }

    private static AnnihilatorRecipe milestone(int amount, String poolSuffix) {
        ItemStack payout = ItemBlueprints.make(MachineItems.BLUEPRINTS, GenericRecipes.POOL_PREFIX_528 + poolSuffix);
        if (payout.isEmpty() || payout.getItem() == Items.AIR) payout = new ItemStack(Items.PAPER);
        return new AnnihilatorRecipe(new Milestone(BigInteger.valueOf(amount), payout));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static final class Milestone {
        public final BigInteger amount;
        public final ItemStack payout;

        public Milestone(BigInteger amount, ItemStack payout) {
            this.amount = amount;
            this.payout = payout;
        }
    }

    public static final class AnnihilatorRecipe {
        public final List<Milestone> milestones = new ArrayList<>();

        public AnnihilatorRecipe(Milestone... milestones) {
            this.milestones.addAll(List.of(milestones));
        }
    }
}
