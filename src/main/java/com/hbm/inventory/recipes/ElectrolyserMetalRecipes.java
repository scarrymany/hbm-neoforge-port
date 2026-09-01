package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreOutput;
import com.hbm.items.special.BedrockOreType;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code com.hbm.inventory.recipes.ElectrolyserMetalRecipes} crystal rows + bedrock loop.
 * {@code chunk_ore_*} flatten is {@code Phase11ProcessItems} ({@code ModItems.java:1273});
 * CE has no {@code chunk_ore*.png} — items are registered without invented art.
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
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_ALUMINIUM.get()), new ElectrolysisMetalRecipe(
                new Mats.MaterialStack(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT.q(2)),
                new Mats.MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(2)),
                new ItemStack(item("chunk_ore_cryolite"), 4),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 3)));
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

        // CE ElectrolyserMetalRecipes.java:132-151 — keep 3 put sites (census counts source lines).
        for (BedrockOreType type : BedrockOreType.VALUES) {
            List<Product> productsF = new ArrayList<>();
            productsF.add(new Product(type.primary1, 8));
            productsF.add(new Product(type.primary2, 4));
            productsF.add(new Product(new ItemStack(BedrockOreItems.get(type, BedrockOreGrade.CRUMBS).get()), 1));
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_FIRST).get()),
                    makeBedrockOreProduct(productsF));

            List<Product> productsS = new ArrayList<>();
            productsS.add(new Product(type.primary1, 4));
            productsS.add(new Product(type.primary2, 8));
            productsS.add(new Product(new ItemStack(BedrockOreItems.get(type, BedrockOreGrade.CRUMBS).get()), 1));
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_SECOND).get()),
                    makeBedrockOreProduct(productsS));

            List<Product> productsC = new ArrayList<>();
            productsC.add(new Product(type.primary1, 2));
            productsC.add(new Product(type.primary2, 2));
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.CRUMBS).get()),
                    makeBedrockOreProduct(productsC));
        }
    }

    /** CE {@code ElectrolyserMetalRecipes.makeBedrockOreProduct} :154-177. */
    public static ElectrolysisMetalRecipe makeBedrockOreProduct(List<Product> products) {
        List<Mats.MaterialStack> molten = new ArrayList<>();
        List<ItemStack> solid = new ArrayList<>();
        for (Product product : products) {
            if (molten.size() < 2 && product.key instanceof BedrockOreOutput out) {
                Mats.MaterialStack melt = toFluid(out, product.qty);
                if (melt != null) {
                    molten.add(melt);
                    continue;
                }
            }
            if (product.key instanceof BedrockOreOutput out) {
                solid.add(extract(out, product.qty));
            }
            if (product.key instanceof ItemStack stack) {
                solid.add(stack.copy());
            }
        }
        if (molten.isEmpty()) {
            molten.add(new Mats.MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(2)));
        }
        return new ElectrolysisMetalRecipe(
                molten.get(0),
                molten.size() > 1 ? molten.get(1) : null,
                20,
                solid.toArray(ItemStack[]::new));
    }

    /** CE {@code ItemBedrockOreNew.toFluid} :278-282. */
    private static Mats.MaterialStack toFluid(BedrockOreOutput o, double amount) {
        if (o != null && o.material() != null && o.material().smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return new Mats.MaterialStack(o.material(), (int) Math.ceil(MaterialShapes.FRAGMENT.q(o.amount()) * amount));
        }
        return null;
    }

    /** CE {@code ItemBedrockOreNew.extract} :285-287 — flattened {@code <mat>_ore_fragment}. */
    private static ItemStack extract(BedrockOreOutput o, double amount) {
        int count = Math.min((int) Math.ceil(o.amount() * amount), 64);
        String id = MaterialShapes.FRAGMENT.buildRegistryName(o.material());
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
        return new ItemStack(item, count);
    }

    public record Product(Object key, int qty) {
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
            this(output1, output2, 600, byproduct);
        }

        public ElectrolysisMetalRecipe(Mats.MaterialStack output1, Mats.MaterialStack output2, int duration, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.byproduct = byproduct;
            this.duration = duration;
        }
    }
}
