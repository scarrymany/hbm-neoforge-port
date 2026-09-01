package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * CE {@code com.hbm.inventory.recipes.ElectrolyserMetalRecipes} crystal rows.
 * Bedrock-ore loop skipped (ItemBedrockOreNew flatten incomplete).
 * {@code crystal_aluminium} skipped — {@code chunk_ore} CRYOLITE is not registered.
 */
public final class ElectrolyserMetalRecipes {

    public static final Map<ComparableStack, ElectrolysisMetalRecipe> RECIPES = new HashMap<>();

    private static boolean registered = false;

    private ElectrolyserMetalRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE ElectrolyserMetalRecipes.java:34-130
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_IRON.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_TITANIUM, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_GOLD.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_GOLD, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_LEAD, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_URANIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_URANIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_RADIUM, MaterialShapes.NUGGET.q(4)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_THORIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_THORIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_URANIUM, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_PLUTONIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_POLONIUM, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TITANIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_TITANIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COPPER.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_COPPER, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_LEAD, MaterialShapes.NUGGET.q(4)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3),
                new ItemStack(item("sulfur"), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TUNGSTEN.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_TUNGSTEN, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        // TODO(CE: com.hbm.inventory.recipes.ElectrolyserMetalRecipes.java:75-79):
        // crystal_aluminium byproduct chunk_ore CRYOLITE — not registered. Do not invent.
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_BERYLLIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_BERYLLIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_LEAD, MaterialShapes.NUGGET.q(4)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LEAD.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_LEAD, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_GOLD, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRARANIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_SCHRABIDIUM, MaterialShapes.NUGGET.q(5)),
                new Mats.MaterialStack(Mats.MAT_URANIUM, MaterialShapes.NUGGET.q(2)),
                new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRABIDIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_SCHRABIDIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_RARE.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_ZIRCONIUM, MaterialShapes.NUGGET.q(6)),
                new Mats.MaterialStack(Mats.MAT_BORON, MaterialShapes.NUGGET.q(2)),
                new ItemStack(BilletPowderItems.POWDER_DESH_MIX.get(), 3)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT.q(3)),
                new Mats.MaterialStack(Mats.MAT_COBALT, MaterialShapes.INGOT.q(4)),
                new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get(), 4),
                new ItemStack(BilletPowderItems.POWDER_NITAN_MIX.get(), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LITHIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_LITHIUM, MaterialShapes.INGOT.q(6)),
                new Mats.MaterialStack(Mats.MAT_BORON, MaterialShapes.INGOT.q(2)),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 2),
                new ItemStack(item("fluorite"), 2)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_STARMETAL.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_DURA, MaterialShapes.INGOT.q(4)),
                new Mats.MaterialStack(Mats.MAT_COBALT, MaterialShapes.INGOT.q(4)),
                new ItemStack(BilletPowderItems.POWDER_ASTATINE.get(), 3),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 8)));
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COBALT.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_COBALT, MaterialShapes.INGOT.q(3)),
                new Mats.MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(4)),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 4),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));

        // TODO(CE: com.hbm.inventory.recipes.ElectrolyserMetalRecipes.java:132-151):
        // ItemBedrockOreNew PRIMARY_FIRST/SECOND/CRUMBS loop — bedrock ore flatten incomplete.
    }

    public static ElectrolysisMetalRecipe getRecipe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        return RECIPES.get(new ComparableStack(stack).makeSingular());
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    public static final class ElectrolysisMetalRecipe {
        public final Mats.MaterialStack output1;
        public final Mats.MaterialStack output2;
        public final ItemStack[] byproduct;
        public final int duration;

        public ElectrolysisMetalRecipe(Mats.MaterialStack output1, Mats.MaterialStack output2, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.byproduct = byproduct;
            this.duration = 600;
        }
    }
}
