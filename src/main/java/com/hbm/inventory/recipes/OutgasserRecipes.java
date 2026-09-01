package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code OutgasserRecipes.java}:35-60 — irradiation table shared by
 * {@code TileEntityRBMKOutgasser} and {@code TileEntityFusionBreeder}.
 */
public final class OutgasserRecipes {

    public static final Map<AStack, OutgasserRecipe> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private OutgasserRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE OutgasserRecipes.java:35-38 lithium → tritium
        RECIPES.put(new ComparableStack(item("lithium")), new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 1_000)));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_LITHIUM.get()), new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 1_000)));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_LITHIUM_TINY.get()), new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 100)));
        RECIPES.put(new ComparableStack(block("lithium_block")), new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 10_000)));

        // :41-43 gold → gold-198
        RECIPES.put(OreDictStack.ofCommonTag("ingots/gold"), new OutgasserRecipe(new ItemStack(IngotNuggetItems.INGOT_AU198.get()), null));
        RECIPES.put(new ComparableStack(Items.GOLD_INGOT), new OutgasserRecipe(new ItemStack(IngotNuggetItems.INGOT_AU198.get()), null));
        RECIPES.put(OreDictStack.ofCommonTag("nuggets/gold"), new OutgasserRecipe(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), null));
        RECIPES.put(new ComparableStack(Items.GOLD_NUGGET), new OutgasserRecipe(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), null));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_GOLD.get()), new OutgasserRecipe(new ItemStack(BilletPowderItems.POWDER_AU198.get()), null));

        // :46-48 TH232 → thorium fuel
        RECIPES.put(new ComparableStack(item("ingot_th232")), new OutgasserRecipe(new ItemStack(IngotNuggetItems.INGOT_THORIUM_FUEL.get()), null));
        RECIPES.put(new ComparableStack(item("nugget_th232")), new OutgasserRecipe(new ItemStack(IngotNuggetItems.NUGGET_THORIUM_FUEL.get()), null));
        RECIPES.put(new ComparableStack(item("billet_th232")), new OutgasserRecipe(new ItemStack(BilletPowderItems.BILLET_THORIUM_FUEL.get()), null));

        // :51-53 mushrooms
        RECIPES.put(new ComparableStack(Blocks.BROWN_MUSHROOM), new OutgasserRecipe(new ItemStack(block("mush")), null));
        RECIPES.put(new ComparableStack(Blocks.RED_MUSHROOM), new OutgasserRecipe(new ItemStack(block("mush")), null));
        RECIPES.put(new ComparableStack(Items.MUSHROOM_STEW), new OutgasserRecipe(new ItemStack(item("glowing_stew")), null));

        // :55-57 coal → tar + syngas
        RECIPES.put(new ComparableStack(Items.COAL), new OutgasserRecipe(new ItemStack(item("oil_tar_coal")), new FluidStack(Fluids.SYNGAS, 50)));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_COAL.get()), new OutgasserRecipe(new ItemStack(item("oil_tar_coal")), new FluidStack(Fluids.SYNGAS, 50)));
        RECIPES.put(new ComparableStack(Blocks.COAL_BLOCK), new OutgasserRecipe(new ItemStack(item("oil_tar_coal"), 9), new FluidStack(Fluids.SYNGAS, 500)));

        // :59-60 oil_tar COAL / WAX
        RECIPES.put(new ComparableStack(item("oil_tar_coal")), new OutgasserRecipe(null, new FluidStack(Fluids.COALOIL, 100)));
        RECIPES.put(new ComparableStack(item("oil_tar_wax")), new OutgasserRecipe(null, new FluidStack(Fluids.RADIOSOLVENT, 100)));

        RECIPES.entrySet().removeIf(e -> {
            ItemStack solid = e.getValue().solidOutput;
            return solid != null && (solid.isEmpty() || solid.getItem() == Items.AIR);
        });
    }

    public static OutgasserRecipe getRecipe(ItemStack input) {
        if (input == null || input.isEmpty()) return null;
        register();
        for (Map.Entry<AStack, OutgasserRecipe> entry : RECIPES.entrySet()) {
            if (entry.getKey().matchesRecipe(input, true)) return entry.getValue();
        }
        return null;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static final class OutgasserRecipe {
        public final ItemStack solidOutput;
        public final FluidStack fluidOutput;
        public final boolean fusionOnly;

        public OutgasserRecipe(ItemStack solidOutput, FluidStack fluidOutput) {
            this(solidOutput, fluidOutput, false);
        }

        public OutgasserRecipe(ItemStack solidOutput, FluidStack fluidOutput, boolean fusionOnly) {
            this.solidOutput = solidOutput;
            this.fluidOutput = fluidOutput;
            this.fusionOnly = fusionOnly;
        }
    }
}
