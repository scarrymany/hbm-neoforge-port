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
 * CE {@code PlasmaForgeRecipes.java}. Generated from CE registerDefaults.
 * {@code setInputEnergy} is ignition HE on complete (no PlasmaNetwork).
 */
public final class PlasmaForgeRecipes {

    public static final List<PlasmaForgeRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private PlasmaForgeRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
        RECIPES.add(new PlasmaForgeRecipe("plsm.plateeuphemium", 1000000L, 600, 10000000L, new ItemStack(item("plate_euphemium"), 4), null, new ComparableStack(item("ingot_euphemium"), 4), new ComparableStack(item("powder_astatine"), 3), new ComparableStack(item("powder_bismuth"), 1), new ComparableStack(item("gem_volcanic"), 1), new ComparableStack(item("ingot_osmiridium"), 1)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.platednt", 1000000L, 600, 10000000L, new ItemStack(item("plate_dineutronium"), 4), null, new ComparableStack(item("ingot_dineutronium"), 4), new ComparableStack(item("powder_spark_mix"), 2), new ComparableStack(item("ingot_desh"), 1)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.hde", 10000000L, 600, 25000000L, new ItemStack(item("part_generic_hde"), 1), new FluidStack(Fluids.STELLAR_FLUX, 4000), new ComparableStack(item("bismuthbronze_plate_triple"), 2), new ComparableStack(item("plate_combine_steel"), 1), new ComparableStack(item("ingot_cft"), 1)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldiron", 500000L, 50, 100L, new ItemStack(item("iron_plate_sextuple"), 1), null, new ComparableStack(item("iron_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldsteel", 500000L, 50, 500L, new ItemStack(item("steel_plate_sextuple"), 1), null, new ComparableStack(item("steel_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldcopper", 500000L, 50, 1000L, new ItemStack(item("copper_plate_sextuple"), 1), null, new ComparableStack(item("copper_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldtitanium", 500000L, 300, 50000L, new ItemStack(item("titanium_plate_sextuple"), 1), null, new ComparableStack(item("titanium_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldzirconium", 500000L, 300, 10000L, new ItemStack(item("zirconium_plate_sextuple"), 1), null, new ComparableStack(item("zirconium_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldtcalloy", 500000L, 600, 1000000L, new ItemStack(item("tcalloy_plate_sextuple"), 1), new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("tcalloy_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldtungsten", 500000L, 600, 250000L, new ItemStack(item("tungsten_plate_sextuple"), 1), new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("tungsten_plate_triple"), 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.icfpress", 1000000L, 800, 10000000L, new ItemStack(item("machine_icf_press"), 1), null, new ComparableStack(item("gold_plate_triple"), 8), new ComparableStack(item("motor"), 4), new ComparableStack(item("circuit_bismoid"), 1)));
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

    public static PlasmaForgeRecipe getRecipe(ItemStack... inputs) {
        registerDefaults();
        outer:
        for (PlasmaForgeRecipe recipe : RECIPES) {
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

    public static class PlasmaForgeRecipe {
        public final String name;
        public final long inputEnergy;
        public final int duration;
        public final long power;
        public final ItemStack output;
        public final FluidStack fluid;
        public final AStack[] ingredients;

        public PlasmaForgeRecipe(String name, long inputEnergy, int duration, long power,
                                 ItemStack output, FluidStack fluid, AStack... ingredients) {
            this.name = name;
            this.inputEnergy = inputEnergy;
            this.duration = duration;
            this.power = power;
            this.output = output;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }
    }
}
