package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * CE {@code LiquefactionRecipes.java:32-69}. Each insert is a live {@code RECIPES.put} for the census.
 * Skipped: lignite gem, {@code glyphid_gland_empty}, {@code plant_flower} metas,
 * {@code PB.block}, food→SALIENT dynamic. Tar keys landed ({@code oil_tar_*} flatten).
 */
public final class LiquefactionRecipes {

    public static final Map<ComparableStack, FluidStack> RECIPES = new HashMap<>();

    private static boolean registered = false;

    private LiquefactionRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // :32 KEY_*_TAR → oil_tar flatten
        RECIPES.put(new ComparableStack(item("oil_tar_coal")), new FluidStack(Fluids.COALOIL, 200));
        RECIPES.put(new ComparableStack(item("oil_tar_wood")), new FluidStack(Fluids.HEATINGOIL, 200));
        RECIPES.put(new ComparableStack(item("oil_tar_wax")), new FluidStack(Fluids.LUBRICANT, 100));
        RECIPES.put(new ComparableStack(item("oil_tar_paraffin")), new FluidStack(Fluids.LUBRICANT, 100));

        // :33-34
        RECIPES.put(new ComparableStack(Items.COAL), new FluidStack(Fluids.COALOIL, 250));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_COAL.get()), new FluidStack(Fluids.COALOIL, 250));
        // :36
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_LIGNITE.get()), new FluidStack(Fluids.COALOIL, 150));
        // :40
        RECIPES.put(new ComparableStack(Items.OAK_LOG), new FluidStack(Fluids.MUG, 100));
        // :41-43
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_SODIUM.get()), new FluidStack(Fluids.SODIUM, 100));
        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_LEAD.get()), new FluidStack(Fluids.LEAD, 100));
        RECIPES.put(new ComparableStack(BilletPowderItems.POWDER_LEAD.get()), new FluidStack(Fluids.LEAD, 100));
        // :46-54
        RECIPES.put(new ComparableStack(Blocks.NETHERRACK.asItem()), new FluidStack(Fluids.LAVA, 250));
        RECIPES.put(new ComparableStack(Blocks.COBBLESTONE.asItem()), new FluidStack(Fluids.LAVA, 250));
        RECIPES.put(new ComparableStack(Blocks.STONE.asItem()), new FluidStack(Fluids.LAVA, 250));
        RECIPES.put(new ComparableStack(Blocks.OBSIDIAN.asItem()), new FluidStack(Fluids.LAVA, 500));
        RECIPES.put(new ComparableStack(Items.SNOWBALL), new FluidStack(Fluids.WATER, 125));
        RECIPES.put(new ComparableStack(Blocks.SNOW_BLOCK.asItem()), new FluidStack(Fluids.WATER, 500));
        RECIPES.put(new ComparableStack(Blocks.ICE.asItem()), new FluidStack(Fluids.WATER, 1_000));
        RECIPES.put(new ComparableStack(Blocks.PACKED_ICE.asItem()), new FluidStack(Fluids.WATER, 1_000));
        RECIPES.put(new ComparableStack(Items.ENDER_PEARL), new FluidStack(Fluids.ENDERJUICE, 100));
        // :55
        RECIPES.put(new ComparableStack(item("ore_oil_sand")), new FluidStack(Fluids.BITUMEN, 100));
        // :57-58
        RECIPES.put(new ComparableStack(Items.SUGAR), new FluidStack(Fluids.ETHANOL, 100));
        RECIPES.put(new ComparableStack(Items.MELON_SLICE), new FluidStack(Fluids.ETHANOL, 100));
        // :61
        RECIPES.put(new ComparableStack(Phase11ProcessItems.BIOMASS.get()), new FluidStack(Fluids.BIOGAS, 125));
        // :63
        RECIPES.put(new ComparableStack(Items.COD), new FluidStack(Fluids.FISHOIL, 100));
        RECIPES.put(new ComparableStack(Items.SALMON), new FluidStack(Fluids.FISHOIL, 100));
        // :64
        RECIPES.put(new ComparableStack(Blocks.SUNFLOWER.asItem()), new FluidStack(Fluids.SUNFLOWEROIL, 100));
        // :66-69
        RECIPES.put(new ComparableStack(Items.WHEAT_SEEDS), new FluidStack(Fluids.SEEDSLURRY, 50));
        RECIPES.put(new ComparableStack(Blocks.SHORT_GRASS.asItem()), new FluidStack(Fluids.SEEDSLURRY, 100));
        RECIPES.put(new ComparableStack(Blocks.FERN.asItem()), new FluidStack(Fluids.SEEDSLURRY, 100));
        RECIPES.put(new ComparableStack(Blocks.VINE.asItem()), new FluidStack(Fluids.SEEDSLURRY, 100));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static FluidStack getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        return RECIPES.get(new ComparableStack(stack.getItem()));
    }
}
