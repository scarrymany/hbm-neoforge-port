package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CE {@code ArcWelderRecipes.java}. Generated from CE registerDefaults.
 */
public final class ArcWelderRecipes {

    public static final List<ArcWelderRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private ArcWelderRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("motor"), 2), 100, 400L, new ComparableStack(item("plate_steel"), 2), new ComparableStack(item("mingrade_dense_wire"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("part_generic_lde"), 1), 200, 5000L, new ComparableStack(item("plate_aluminium"), 4), new ComparableStack(item("ingot_fiberglass"), 4), OreDictStack.ofHbmTag("any_hardplastic", 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("part_generic_lde"), 1), 200, 10000L, new ComparableStack(item("plate_titanium"), 2), new ComparableStack(item("ingot_fiberglass"), 4), OreDictStack.ofHbmTag("any_hardplastic", 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("neutron_reflector"), 2), 400, 50000L, new ComparableStack(item("ingot_tungsten_carbide"), 2), new ComparableStack(item("plate_dura_steel"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("copper_dense_wire"), 1), 100, 10000L, new ComparableStack(item("copper_wire"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("mingrade_dense_wire"), 1), 100, 10000L, new ComparableStack(item("mingrade_wire"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("gold_dense_wire"), 1), 100, 10000L, new ComparableStack(item("gold_wire"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("iron_plate_sextuple"), 1), 100, 100L, new ComparableStack(item("iron_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("steel_plate_sextuple"), 1), 100, 500L, new ComparableStack(item("steel_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("copper_plate_sextuple"), 1), 200, 1000L, new ComparableStack(item("copper_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("titanium_plate_sextuple"), 1), 600, 50000L, new ComparableStack(item("titanium_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("zirconium_plate_sextuple"), 1), 600, 10000L, new ComparableStack(item("zirconium_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("aluminum_plate_sextuple"), 1), 300, 10000L, new ComparableStack(item("aluminum_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("tcalloy_plate_sextuple"), 1), 1200, 1000000L, new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("tcalloy_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("cdalloy_plate_sextuple"), 1), 1200, 1000000L, new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("cdalloy_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("tungsten_plate_sextuple"), 1), 1200, 250000L, new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("tungsten_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("cmbsteel_plate_sextuple"), 1), 1200, 10000000L, new FluidStack(Fluids.REFORMGAS, 1000), new ComparableStack(item("plate_combine_steel"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("osmiridium_plate_sextuple"), 1), 6000, 20000000L, new FluidStack(Fluids.REFORMGAS, 16000), new ComparableStack(item("osmiridium_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("thruster_small"), 1), 60, 1000L, new ComparableStack(item("plate_steel"), 4), new ComparableStack(item("aluminum_wire"), 4), new ComparableStack(item("plate_copper"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("thruster_medium"), 1), 100, 2000L, new ComparableStack(item("plate_steel"), 8), new ComparableStack(item("motor"), 1), new ComparableStack(item("ingot_graphite"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("thruster_large"), 1), 200, 5000L, new ComparableStack(item("ingot_dura_steel"), 10), new ComparableStack(item("motor"), 1), new ComparableStack(item("neutron_reflector"), 12)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("fuel_tank_small"), 1), 60, 1000L, new ComparableStack(item("plate_aluminium"), 6), new ComparableStack(item("plate_copper"), 4), new ComparableStack(item("steel_scaffold"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("fuel_tank_medium"), 1), 100, 2000L, new ComparableStack(item("aluminum_plate_triple"), 4), new ComparableStack(item("plate_titanium"), 8), new ComparableStack(item("steel_scaffold"), 12)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("fuel_tank_large"), 1), 200, 5000L, new ComparableStack(item("aluminum_plate_sextuple"), 8), new ComparableStack(item("plate_saturnite"), 12), new ComparableStack(item("steel_scaffold"), 16)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_anti_ballistic"), 1), 100, 5000L, OreDictStack.ofHbmTag("any_highexplosive", 3), new ComparableStack(item("missile_assembly"), 1), new ComparableStack(item("thruster_small"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_generic"), 1), 100, 5000L, new ComparableStack(item("warhead_generic_small"), 1), new ComparableStack(item("fuel_tank_small"), 1), new ComparableStack(item("thruster_small"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_incendiary"), 1), 100, 5000L, new ComparableStack(item("warhead_incendiary_small"), 1), new ComparableStack(item("fuel_tank_small"), 1), new ComparableStack(item("thruster_small"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_cluster"), 1), 100, 5000L, new ComparableStack(item("warhead_cluster_small"), 1), new ComparableStack(item("fuel_tank_small"), 1), new ComparableStack(item("thruster_small"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_buster"), 1), 100, 5000L, new ComparableStack(item("warhead_buster_small"), 1), new ComparableStack(item("fuel_tank_small"), 1), new ComparableStack(item("thruster_small"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_decoy"), 1), 60, 2500L, new ComparableStack(item("ingot_steel"), 1), new ComparableStack(item("fuel_tank_small"), 1), new ComparableStack(item("thruster_small"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_strong"), 1), 200, 10000L, new ComparableStack(item("warhead_generic_medium"), 1), new ComparableStack(item("fuel_tank_medium"), 1), new ComparableStack(item("thruster_medium"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_incendiary_strong"), 1), 200, 10000L, new ComparableStack(item("warhead_incendiary_medium"), 1), new ComparableStack(item("fuel_tank_medium"), 1), new ComparableStack(item("thruster_medium"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_cluster_strong"), 1), 200, 10000L, new ComparableStack(item("warhead_cluster_medium"), 1), new ComparableStack(item("fuel_tank_medium"), 1), new ComparableStack(item("thruster_medium"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_buster_strong"), 1), 200, 10000L, new ComparableStack(item("warhead_buster_medium"), 1), new ComparableStack(item("fuel_tank_medium"), 1), new ComparableStack(item("thruster_medium"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_emp_strong"), 1), 200, 10000L, new ComparableStack(item("emp_bomb"), 3), new ComparableStack(item("fuel_tank_medium"), 1), new ComparableStack(item("thruster_medium"), 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_burst"), 1), 300, 25000L, new ComparableStack(item("warhead_generic_large"), 1), new ComparableStack(item("fuel_tank_medium"), 2), new ComparableStack(item("thruster_medium"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_inferno"), 1), 300, 25000L, new ComparableStack(item("warhead_incendiary_large"), 1), new ComparableStack(item("fuel_tank_medium"), 2), new ComparableStack(item("thruster_medium"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_rain"), 1), 300, 25000L, new ComparableStack(item("warhead_cluster_large"), 1), new ComparableStack(item("fuel_tank_medium"), 2), new ComparableStack(item("thruster_medium"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_drill"), 1), 300, 25000L, new ComparableStack(item("warhead_buster_large"), 1), new ComparableStack(item("fuel_tank_medium"), 2), new ComparableStack(item("thruster_medium"), 4)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_nuclear"), 1), 600, 50000L, new ComparableStack(item("warhead_nuclear"), 1), new ComparableStack(item("fuel_tank_large"), 1), new ComparableStack(item("thruster_large"), 3)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_nuclear_cluster"), 1), 600, 50000L, new ComparableStack(item("warhead_mirv"), 1), new ComparableStack(item("fuel_tank_large"), 1), new ComparableStack(item("thruster_large"), 3)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("missile_volcano"), 1), 600, 50000L, new ComparableStack(item("warhead_volcano"), 1), new ComparableStack(item("fuel_tank_large"), 1), new ComparableStack(item("thruster_large"), 3)));
    }

    private static Item item(String id) {
        if (id.contains(":")) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static synchronized void register() {
        registerDefaults();
    }

    public static ArcWelderRecipe getRecipe(ItemStack... inputs) {
        registerDefaults();
        outer:
        for (ArcWelderRecipe recipe : RECIPES) {
            List<AStack> left = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack in : inputs) {
                if (in.isEmpty()) continue;
                boolean hit = false;
                for (int i = 0; i < left.size(); i++) {
                    AStack key = left.get(i);
                    if (key.matchesRecipe(in, true) && in.getCount() >= key.count()) {
                        left.remove(i);
                        hit = true;
                        break;
                    }
                }
                if (!hit) continue outer;
            }
            if (left.isEmpty()) return recipe;
        }
        return null;
    }

    public static class ArcWelderRecipe {
        public final AStack[] ingredients;
        public final FluidStack fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }
    }
}
