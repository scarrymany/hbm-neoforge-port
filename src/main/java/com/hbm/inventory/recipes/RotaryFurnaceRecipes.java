package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hbm.inventory.material.MaterialShapes.INGOT;

/**
 * CE {@code RotaryFurnaceRecipes.java}:30-47. Census: {@code RECIPES.add}.
 */
public final class RotaryFurnaceRecipes {

    public static final List<RotaryFurnaceRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private RotaryFurnaceRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE RotaryFurnaceRecipes.java:32-46
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_STEEL, INGOT.q(1)), 100, 100,
                tag("ingots/iron", 1), tag("gems/coal", 1)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_STEEL, INGOT.q(1)), 100, 100,
                tag("ingots/iron", 1), OreDictStack.ofHbmTag("any_coke", 1)));

        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_STEEL, INGOT.q(2)), 200, 25,
                tag("ore_fragments/iron", 9), tag("gems/coal", 1)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_STEEL, INGOT.q(3)), 200, 25,
                tag("ore_fragments/iron", 9), OreDictStack.ofHbmTag("any_coke", 1)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_STEEL, INGOT.q(4)), 400, 25,
                tag("ore_fragments/iron", 9), OreDictStack.ofHbmTag("any_coke", 1), cmp("powder_flux")));

        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_DESH, INGOT.q(1)), 100, 200,
                new FluidStack(Fluids.LIGHTOIL, 100), cmp("powder_desh_ready")));

        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_GUNMETAL, INGOT.q(4)), 200, 100,
                tag("ingots/copper", 3), tag("ingots/aluminum", 1)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_WEAPONSTEEL, INGOT.q(1)), 200, 400,
                new FluidStack(Fluids.GAS_COKER, 100), tag("ingots/steel", 1), cmp("powder_flux", 2)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_SATURN, INGOT.q(2)), 200, 400,
                new FluidStack(Fluids.REFORMGAS, 250), tag("dusts/dura_steel", 4), tag("dusts/copper", 1)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_SATURN, INGOT.q(4)), 200, 300,
                new FluidStack(Fluids.REFORMGAS, 250), tag("dusts/dura_steel", 4), tag("dusts/copper", 1), tag("dusts/borax", 1)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_ALUMINIUM, INGOT.q(2)), 100, 400,
                new FluidStack(Fluids.SODIUM_ALUMINATE, 150)));
        RECIPES.add(new RotaryFurnaceRecipe(new MaterialStack(Mats.MAT_ALUMINIUM, INGOT.q(3)), 40, 200,
                new FluidStack(Fluids.SODIUM_ALUMINATE, 150), cmp("powder_flux", 2)));
    }

    public static RotaryFurnaceRecipe getRecipe(ItemStack... inputs) {
        register();
        outer:
        for (RotaryFurnaceRecipe recipe : RECIPES) {
            List<AStack> remaining = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack input : inputs) {
                if (input == null || input.isEmpty()) continue;
                boolean match = false;
                for (int i = 0; i < remaining.size(); i++) {
                    AStack need = remaining.get(i);
                    if (need.matchesRecipe(input, true) && input.getCount() >= need.stacksize) {
                        remaining.remove(i);
                        match = true;
                        break;
                    }
                }
                if (!match) continue outer;
            }
            if (remaining.isEmpty()) return recipe;
        }
        return null;
    }

    private static OreDictStack tag(String path, int n) {
        return new OreDictStack(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path)), n);
    }

    private static ComparableStack cmp(String id) {
        return cmp(id, 1);
    }

    private static ComparableStack cmp(String id, int n) {
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
        return new ComparableStack(i == Items.AIR ? Items.AIR : i, n);
    }

    public static final class RotaryFurnaceRecipe {
        public final AStack[] ingredients;
        public final FluidStack fluid;
        public final MaterialStack output;
        public final int duration;
        public final int steam;

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, FluidStack fluid, AStack... ingredients) {
            this.ingredients = ingredients;
            this.fluid = fluid;
            this.output = output;
            this.duration = duration;
            this.steam = steam;
        }

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, AStack... ingredients) {
            this(output, duration, steam, null, ingredients);
        }
    }
}
