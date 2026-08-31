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
 * CE {@code LiquefactionRecipes.java:32-69}. {@code RECIPES.put} so the census hits.
 * Skipped: {@code KEY_*_TAR} ({@code oil_tar} unregistered), lignite gem, {@code glyphid_gland_empty},
 * {@code plant_flower} metas, {@code PB.block} (block_lead not registered), food→SALIENT dynamic.
 */
public final class LiquefactionRecipes {

    public static final Map<ComparableStack, FluidStack> RECIPES = new HashMap<>();

    private static boolean registered = false;

    private LiquefactionRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // :33-34
        put(Items.COAL, new FluidStack(Fluids.COALOIL, 250));
        put(BilletPowderItems.POWDER_COAL.get(), new FluidStack(Fluids.COALOIL, 250));
        // :36 lignite dust only
        put(BilletPowderItems.POWDER_LIGNITE.get(), new FluidStack(Fluids.COALOIL, 150));
        // :40 KEY_LOG
        put(Items.OAK_LOG, new FluidStack(Fluids.MUG, 100));
        // :41-43
        put(BilletPowderItems.POWDER_SODIUM.get(), new FluidStack(Fluids.SODIUM, 100));
        put(IngotNuggetItems.INGOT_LEAD.get(), new FluidStack(Fluids.LEAD, 100));
        put(BilletPowderItems.POWDER_LEAD.get(), new FluidStack(Fluids.LEAD, 100));
        // :46-54
        put(Blocks.NETHERRACK.asItem(), new FluidStack(Fluids.LAVA, 250));
        put(Blocks.COBBLESTONE.asItem(), new FluidStack(Fluids.LAVA, 250));
        put(Blocks.STONE.asItem(), new FluidStack(Fluids.LAVA, 250));
        put(Blocks.OBSIDIAN.asItem(), new FluidStack(Fluids.LAVA, 500));
        put(Items.SNOWBALL, new FluidStack(Fluids.WATER, 125));
        put(Blocks.SNOW_BLOCK.asItem(), new FluidStack(Fluids.WATER, 500));
        put(Blocks.ICE.asItem(), new FluidStack(Fluids.WATER, 1_000));
        put(Blocks.PACKED_ICE.asItem(), new FluidStack(Fluids.WATER, 1_000));
        put(Items.ENDER_PEARL, new FluidStack(Fluids.ENDERJUICE, 100));
        // :55
        put(item("ore_oil_sand"), new FluidStack(Fluids.BITUMEN, 100));
        // :57-58
        put(Items.SUGAR, new FluidStack(Fluids.ETHANOL, 100));
        put(Items.MELON_SLICE, new FluidStack(Fluids.ETHANOL, 100));
        // :61
        put(Phase11ProcessItems.BIOMASS.get(), new FluidStack(Fluids.BIOGAS, 125));
        // :63 fish wildcard → cod
        put(Items.COD, new FluidStack(Fluids.FISHOIL, 100));
        put(Items.SALMON, new FluidStack(Fluids.FISHOIL, 100));
        // :64 sunflower
        put(Blocks.SUNFLOWER.asItem(), new FluidStack(Fluids.SUNFLOWEROIL, 100));
        // :66-69
        put(Items.WHEAT_SEEDS, new FluidStack(Fluids.SEEDSLURRY, 50));
        put(Blocks.SHORT_GRASS.asItem(), new FluidStack(Fluids.SEEDSLURRY, 100));
        put(Blocks.FERN.asItem(), new FluidStack(Fluids.SEEDSLURRY, 100));
        put(Blocks.VINE.asItem(), new FluidStack(Fluids.SEEDSLURRY, 100));
    }

    private static void put(Item item, FluidStack out) {
        if (item == null || item == Items.AIR) return;
        RECIPES.put(new ComparableStack(item), out);
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
