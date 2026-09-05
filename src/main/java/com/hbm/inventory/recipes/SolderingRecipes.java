package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
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
        // CE SolderingRecipes.java:36-72 — circuits ANALOG, BASIC, ADVANCED (no-lbsm counts)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_analog"), 1), 100, 100, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 3), new ComparableStack(item("circuit_capacitor"), 2)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 4)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 4)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_basic"), 1), 200, 250, new AStack[]{new ComparableStack(item("circuit_chip"), 4)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 4)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 4)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_advanced"), 1), 300, 1000, new FluidStack(Fluids.SULFURIC_ACID, 1000), new AStack[]{new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 8), new ComparableStack(item("ingot_rubber"), 2)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 8)}));
        
        // CE :74-82 CAPACITOR_BOARD
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_capacitor_board"), 1), 200, 300, new FluidStack(Fluids.PEROXIDE, 250), new AStack[]{new ComparableStack(item("circuit_capacitor_tantalium"), 3)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 1)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 3)}));
        
        // CE :84-100 BISMOID (no-lbsm: 4 CHIP_BISMOID, 16 CHIP, 24 CAPACITOR → 12 PCB, 2 ANY_HARDPLASTIC.ingot + 12 PB wire + 1000mB SOLVENT)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_bismoid"), 1), 400, 10000, new FluidStack(Fluids.SOLVENT, 1000), new AStack[]{new ComparableStack(item("circuit_chip_bismoid"), 4), new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 24)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 12), new OreDictStack(MaterialShapes.INGOT.commonTag(Mats.MAT_HARDPLASTIC), 2)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 12)}));
        
        // CE :102-119 QUANTUM (no-lbsm: 4 CHIP_QUANTUM, 16 CHIP_BISMOID, 4 ATOMIC_CLOCK → 16 PCB, 4 ANY_HARDPLASTIC.ingot + 16 PB wire + 1000mB HELIUM4)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_quantum"), 1), 400, 100000, new FluidStack(Fluids.HELIUM4, 1000), new AStack[]{new ComparableStack(item("circuit_chip_quantum"), 4), new ComparableStack(item("circuit_chip_bismoid"), 16), new ComparableStack(item("circuit_atomic_clock"), 4)}, new AStack[]{new ComparableStack(item("circuit_pcb"), 16), new OreDictStack(MaterialShapes.INGOT.commonTag(Mats.MAT_HARDPLASTIC), 4)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 16)}));
        
        // CE :127-186 CONTROLLER/CONTROLLER_ADVANCED/CONTROLLER_QUANTUM (no-lbsm)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_controller"), 1), 400, 15000, new FluidStack(Fluids.PERFLUOROMETHYL, 1000), new AStack[]{new ComparableStack(item("circuit_chip"), 32), new ComparableStack(item("circuit_capacitor"), 32), new ComparableStack(item("circuit_capacitor_tantalium"), 16)}, new AStack[]{new ComparableStack(item("circuit_controller_chassis"), 1), new ComparableStack(item("upgrade_speed_1"), 1)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 16)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_controller_advanced"), 1), 600, 25000, new FluidStack(Fluids.PERFLUOROMETHYL, 4000), new AStack[]{new ComparableStack(item("circuit_chip_bismoid"), 16), new ComparableStack(item("circuit_capacitor_tantalium"), 48), new ComparableStack(item("circuit_atomic_clock"), 1)}, new AStack[]{new ComparableStack(item("circuit_controller_chassis"), 1), new ComparableStack(item("upgrade_speed_3"), 1)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 24)}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("circuit_controller_quantum"), 1), 600, 250000, new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6000), new AStack[]{new ComparableStack(item("circuit_chip_quantum"), 16), new ComparableStack(item("circuit_chip_bismoid"), 48), new ComparableStack(item("circuit_atomic_clock"), 8)}, new AStack[]{new ComparableStack(item("circuit_controller_advanced"), 2), new ComparableStack(item("upgrade_overdrive_1"), 1)}, new AStack[]{new OreDictStack(MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD), 32)}));
        
        // CE :191-282 UPGRADES tier 1 (vacuum_tube + capacitor + upgrade_template + dust)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_speed_1"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 4), new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(item("powder_red_copper"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_effect_1"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 4), new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(item("powder_emerald"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_power_1"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 4), new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(item("powder_gold"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_fortune_1"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 4), new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(item("powder_niobium"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_afterburn_1"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_vacuum_tube"), 4), new ComparableStack(item("circuit_capacitor"), 1)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(item("powder_tungsten"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_radius"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_chip"), 4), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(Items.GLOWSTONE_DUST, 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_health"), 1), 200, 1000, new AStack[]{new ComparableStack(item("circuit_chip"), 4), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_template"), 1), new ComparableStack(item("powder_lithium"), 4)}, new AStack[]{}));
        
        // CE :285-295 UPGRADES tier 2 (8 chip + 4 capacitor + tier1 + 4 ANY_PLASTIC, no-lbsm)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_speed_2"), 1), 300, 10000, new AStack[]{new ComparableStack(item("circuit_chip"), 8), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_speed_1"), 1), OreDictStack.ofHbmTag("any_plastic", 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_effect_2"), 1), 300, 10000, new AStack[]{new ComparableStack(item("circuit_chip"), 8), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_effect_1"), 1), OreDictStack.ofHbmTag("any_plastic", 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_power_2"), 1), 300, 10000, new AStack[]{new ComparableStack(item("circuit_chip"), 8), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_power_1"), 1), OreDictStack.ofHbmTag("any_plastic", 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_fortune_2"), 1), 300, 10000, new AStack[]{new ComparableStack(item("circuit_chip"), 8), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_fortune_1"), 1), OreDictStack.ofHbmTag("any_plastic", 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_afterburn_2"), 1), 300, 10000, new AStack[]{new ComparableStack(item("circuit_chip"), 8), new ComparableStack(item("circuit_capacitor"), 4)}, new AStack[]{new ComparableStack(item("upgrade_afterburn_1"), 1), OreDictStack.ofHbmTag("any_plastic", 4)}, new AStack[]{}));
        
        // CE :285-295 UPGRADES tier 3 (16 chip + 16 capacitor + tier2 + 4 rubber + 500mB SOLVENT, no-lbsm)
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_speed_3"), 1), 400, 25000, new FluidStack(Fluids.SOLVENT, 500), new AStack[]{new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 16)}, new AStack[]{new ComparableStack(item("upgrade_speed_2"), 1), new ComparableStack(item("ingot_rubber"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_effect_3"), 1), 400, 25000, new FluidStack(Fluids.SOLVENT, 500), new AStack[]{new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 16)}, new AStack[]{new ComparableStack(item("upgrade_effect_2"), 1), new ComparableStack(item("ingot_rubber"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_power_3"), 1), 400, 25000, new FluidStack(Fluids.SOLVENT, 500), new AStack[]{new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 16)}, new AStack[]{new ComparableStack(item("upgrade_power_2"), 1), new ComparableStack(item("ingot_rubber"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_fortune_3"), 1), 400, 25000, new FluidStack(Fluids.SOLVENT, 500), new AStack[]{new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 16)}, new AStack[]{new ComparableStack(item("upgrade_fortune_2"), 1), new ComparableStack(item("ingot_rubber"), 4)}, new AStack[]{}));
        RECIPES.add(new SolderingRecipe(new ItemStack(item("upgrade_afterburn_3"), 1), 400, 25000, new FluidStack(Fluids.SOLVENT, 500), new AStack[]{new ComparableStack(item("circuit_chip"), 16), new ComparableStack(item("circuit_capacitor"), 16)}, new AStack[]{new ComparableStack(item("upgrade_afterburn_2"), 1), new ComparableStack(item("ingot_rubber"), 4)}, new AStack[]{}));
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
