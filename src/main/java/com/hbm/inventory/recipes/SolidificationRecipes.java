package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Flammable;
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
 * CE {@code SolidificationRecipes.java:63-116}. Each insert is a live {@code RECIPES.put} for the census.
 * {@code registerSFAuto} formula reproduced from {@code :119-134} ({@code tuPerSF * 1000 * 1.25 / heatEnergy}).
 */
public final class SolidificationRecipes {

    public static final int SF_OIL = 200;
    public static final int SF_CRACK = 200;
    public static final int SF_HEAVY = 150;
    public static final int SF_BITUMEN = 100;
    public static final int SF_COALOIL = 200;
    public static final int SF_CREOSOTE = 200;
    public static final int SF_WOOD = 1000;
    public static final int SF_LUBE = 100;

    public static final Map<FluidType, Output> RECIPES = new HashMap<>();

    private static boolean registered = false;

    private SolidificationRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // :63-73
        RECIPES.put(Fluids.WATER, out(1000, new ItemStack(Blocks.ICE)));
        RECIPES.put(Fluids.LAVA, out(1000, new ItemStack(Blocks.OBSIDIAN)));
        RECIPES.put(Fluids.MERCURY, out(125, new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get())));
        RECIPES.put(Fluids.BIOGAS, out(250, new ItemStack(Phase11ProcessItems.BIOMASS_COMPRESSED.get(), 4)));
        RECIPES.put(Fluids.SALIENT, out(1280, new ItemStack(Phase11ProcessItems.BIO_WAFER.get(), 8)));
        RECIPES.put(Fluids.ENDERJUICE, out(100, new ItemStack(Items.ENDER_PEARL)));
        RECIPES.put(Fluids.WATZ, out(1000, new ItemStack(IngotNuggetItems.INGOT_MUD.get())));
        RECIPES.put(Fluids.REDMUD, out(450, new ItemStack(Items.IRON_INGOT)));
        RECIPES.put(Fluids.SODIUM, out(100, new ItemStack(BilletPowderItems.POWDER_SODIUM.get())));
        RECIPES.put(Fluids.LEAD, out(100, new ItemStack(IngotNuggetItems.INGOT_LEAD.get())));
        RECIPES.put(Fluids.SLOP, out(250, new ItemStack(item("ore_oil_sand"))));

        // :75-83 oil_tar flatten
        RECIPES.put(Fluids.OIL, out(SF_OIL, tar("crude")));
        RECIPES.put(Fluids.CRACKOIL, out(SF_CRACK, tar("crack")));
        RECIPES.put(Fluids.COALOIL, out(SF_COALOIL, tar("coal")));
        RECIPES.put(Fluids.HEAVYOIL, out(SF_HEAVY, tar("crude")));
        RECIPES.put(Fluids.HEAVYOIL_VACUUM, out(SF_HEAVY, tar("crude")));
        RECIPES.put(Fluids.BITUMEN, out(SF_BITUMEN, tar("crude")));
        RECIPES.put(Fluids.COALCREOSOTE, out(SF_CREOSOTE, tar("coal")));
        RECIPES.put(Fluids.WOODOIL, out(SF_WOOD, tar("wood")));
        RECIPES.put(Fluids.LUBRICANT, out(SF_LUBE, tar("paraffin")));

        // :85 then overwritten by :115 — keep both puts so the census matches CE's two calls
        RECIPES.put(Fluids.BALEFIRE, out(250, new ItemStack(Phase11ProcessItems.SOLID_FUEL_BF.get())));

        registerSFAuto(Fluids.SMEAR);
        registerSFAuto(Fluids.HEATINGOIL);
        registerSFAuto(Fluids.HEATINGOIL_VACUUM);
        registerSFAuto(Fluids.RECLAIMED);
        registerSFAuto(Fluids.PETROIL);
        registerSFAuto(Fluids.NAPHTHA);
        registerSFAuto(Fluids.NAPHTHA_CRACK);
        registerSFAuto(Fluids.DIESEL);
        registerSFAuto(Fluids.DIESEL_REFORM);
        registerSFAuto(Fluids.DIESEL_CRACK);
        registerSFAuto(Fluids.DIESEL_CRACK_REFORM);
        registerSFAuto(Fluids.LIGHTOIL);
        registerSFAuto(Fluids.LIGHTOIL_CRACK);
        registerSFAuto(Fluids.LIGHTOIL_VACUUM);
        registerSFAuto(Fluids.KEROSENE);
        registerSFAuto(Fluids.KEROSENE_REFORM);
        registerSFAuto(Fluids.SOURGAS);
        registerSFAuto(Fluids.REFORMGAS);
        registerSFAuto(Fluids.SYNGAS);
        registerSFAuto(Fluids.PETROLEUM);
        registerSFAuto(Fluids.LPG);
        registerSFAuto(Fluids.BIOFUEL);
        registerSFAuto(Fluids.AROMATICS);
        registerSFAuto(Fluids.UNSATURATEDS);
        registerSFAuto(Fluids.REFORMATE);
        registerSFAuto(Fluids.XYLENE);
        registerSFAuto(Fluids.BALEFIRE, 24_000_000L, Phase11ProcessItems.SOLID_FUEL_BF.get());
    }

    public static void registerSFAuto(FluidType fluid) {
        registerSFAuto(fluid, 1_440_000L, Phase11ProcessItems.SOLID_FUEL.get());
    }

    public static void registerSFAuto(FluidType fluid, long tuPerSF, Item fuel) {
        FT_Flammable trait = fluid.getTrait(FT_Flammable.class);
        if (trait == null || trait.getHeatEnergy() <= 0) return;
        long tuPerBucket = trait.getHeatEnergy();
        double penalty = 1.25D;
        int mB = (int) (tuPerSF * 1000L * penalty / tuPerBucket);
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);
        mB = Math.max(mB, 1);
        RECIPES.put(fluid, out(mB, new ItemStack(fuel)));
    }

    public static Output getOutput(FluidType type) {
        register();
        return RECIPES.get(type);
    }

    private static Output out(int mB, ItemStack stack) {
        return new Output(mB, stack);
    }

    private static ItemStack tar(String type) {
        return new ItemStack(item("oil_tar_" + type));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public record Output(int amount, ItemStack stack) {
    }
}
