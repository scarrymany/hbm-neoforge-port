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
 * CE {@code SolderingRecipes.java}. Generated from CE registerDefaults (no 528/LBSM forks).
 */
public final class SolderingRecipes {

    public static final List<SolderingRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private SolderingRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_analog"), 1), 100, 100, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 1)}, new AStack[]{new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 1)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_basic"), 1), 200, 250, new AStack[]{new ComparableStack(item("circuit_chip"), 1)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 1)}, new AStack[]{new ComparableStack(item("lead_wire"), 4)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_advanced"), 1), 300, 1000, new FluidStack(Fluids.SULFURIC_ACID, 1000), new AStack[]{new ComparableStack(item("circuit_chip"), 1)}, new AStack[]{new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 1)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_capacitor_board"), 1), 200, 300, new FluidStack(Fluids.PEROXIDE, 250), new AStack[]{new ComparableStack(item("circuit_capacitor_tantalium"), 1)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 1)}, new AStack[]{new ComparableStack(item("lead_wire"), 3)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_bismoid"), 1), 400, 10000, new FluidStack(Fluids.SOLVENT, 1000), new AStack[]{new ComparableStack(item("circuit_chip_bismoid"), 1)}, new AStack[]{new ComparableStack(item("circuit_chip"), 1)}, new AStack[]{new ComparableStack(item("circuit_capacitor"), 1)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_quantum"), 1), 400, 100000, new FluidStack(Fluids.HELIUM4, 1000), new AStack[]{new ComparableStack(item("circuit_chip_quantum"), 1)}, new AStack[]{new ComparableStack(item("circuit_chip_bismoid"), 1)}, new AStack[]{new ComparableStack(item("circuit_atomic_clock"), 1)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_controller"), 1), 400, 15000, new FluidStack(Fluids.PERFLUOROMETHYL, 1000), new AStack[]{new ComparableStack(item("circuit_chip"), 1)}, new AStack[]{new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("circuit_capacitor_tantalium"), 1)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_controller_advanced"), 1), 600, 25000, new FluidStack(Fluids.PERFLUOROMETHYL, 4000), new AStack[]{new ComparableStack(item("circuit_chip_bismoid"), 1)}, new AStack[]{new ComparableStack(item("circuit_capacitor_tantalium"), 1)}, new AStack[]{new ComparableStack(item("circuit_atomic_clock"), 1)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_controller_quantum"), 1), 600, 250000, new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6000), new AStack[]{new ComparableStack(item("circuit_chip_quantum"), 1)}, new AStack[]{new ComparableStack(item("circuit_chip_bismoid"), 1)}, new AStack[]{new ComparableStack(item("circuit_atomic_clock"), 1)}));
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

    public static SolderingRecipe getRecipe(ItemStack[] inputs) {
        registerDefaults();
        for (SolderingRecipe recipe : RECIPES) {
            if (matches(new ItemStack[]{inputs[0], inputs[1], inputs[2]}, recipe.toppings)
                    && matches(new ItemStack[]{inputs[3], inputs[4]}, recipe.pcb)
                    && matches(new ItemStack[]{inputs[5]}, recipe.solder)) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matches(ItemStack[] stacks, AStack[] keys) {
        boolean[] used = new boolean[stacks.length];
        for (AStack key : keys) {
            boolean found = false;
            for (int i = 0; i < stacks.length; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(stacks[i], false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static class SolderingRecipe {
        public final ItemStack output;
        public final int duration;
        public final long consumption;
        public final FluidStack fluid;
        public final AStack[] toppings;
        public final AStack[] pcb;
        public final AStack[] solder;

        public SolderingRecipe(ItemStack output, int duration, long consumption,
                               AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this(output, duration, consumption, null, toppings, pcb, solder);
        }

        public SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                               AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.toppings = toppings;
            this.pcb = pcb;
            this.solder = solder;
        }
    }
}
