package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreOutput;
import com.hbm.items.special.BedrockOreType;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CE {@code ArcFurnaceRecipes.java}:41-115. Census: {@code .register(new} at each hand-written /
 * bedrock site. Material-shape autogen stays a loop.
 * Vanilla furnace autogen skipped — RecipeManager is not available at commonSetup.
 */
public final class ArcFurnaceRecipes {

    public static final List<Tuple.Pair<AStack, ArcFurnaceRecipe>> recipeList = new ArrayList<>();
    public static final Map<ComparableStack, ArcFurnaceRecipe> fastCacheSolid = new HashMap<>();
    public static final Map<ComparableStack, ArcFurnaceRecipe> fastCacheLiquid = new HashMap<>();
    public static final Set<ComparableStack> occupiedSolid = new HashSet<>();
    public static final Set<ComparableStack> occupiedLiquid = new HashSet<>();

    private static boolean registered = false;

    private ArcFurnaceRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE ArcFurnaceRecipes.java:41-53 silica / glass / borax
        ArcFurnaceRecipes.register(new ComparableStack(Items.SAND), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get()))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(1))));
        ArcFurnaceRecipes.register(new ComparableStack(Items.FLINT), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 4))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        ArcFurnaceRecipes.register(new OreDictStack(c("gems/quartz")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 3))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(3))));
        ArcFurnaceRecipes.register(new OreDictStack(c("dusts/quartz")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 3))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(3))));
        ArcFurnaceRecipes.register(new OreDictStack(c("storage_blocks/quartz")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 12))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(12))));
        ArcFurnaceRecipes.register(new OreDictStack(c("ingots/fiberglass")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 4))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        ArcFurnaceRecipes.register(new OreDictStack(c("storage_blocks/fiberglass")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 40))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(9, 2))));
        ArcFurnaceRecipes.register(new OreDictStack(c("ingots/asbestos")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 4))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        ArcFurnaceRecipes.register(new OreDictStack(c("dusts/asbestos")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 4))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        ArcFurnaceRecipes.register(new OreDictStack(c("storage_blocks/asbestos")), new ArcFurnaceRecipe()
                .solid(new ItemStack(IngotNuggetItems.NUGGET_SILICON.get(), 40))
                .fluid(new Mats.MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(9, 2))));
        Item glassQuartz = item("glass_quartz");
        if (glassQuartz != Items.AIR) {
            Item sandQuartz = item("sand_quartz");
            if (sandQuartz != Items.AIR) {
                ArcFurnaceRecipes.register(new ComparableStack(sandQuartz), new ArcFurnaceRecipe().solid(new ItemStack(glassQuartz)));
            }
        }
        ArcFurnaceRecipes.register(new OreDictStack(c("dusts/borax")), new ArcFurnaceRecipe()
                .solid(new ItemStack(item("powder_boron_tiny"), 3))
                .fluid(new Mats.MaterialStack(Mats.MAT_BORON, MaterialShapes.NUGGET.q(3))));

        // CE ArcFurnaceRecipes.java:55-71 bedrock loop — 12 register(new per type
        for (BedrockOreType type : BedrockOreType.VALUES) {
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SULFURIC_BYPRODUCT).get()),
                    new ArcFurnaceRecipe().solid(ore(type, BedrockOreGrade.SULFURIC_ARC, 2)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SULFURIC_ROASTED).get()),
                    new ArcFurnaceRecipe().solid(ore(type, BedrockOreGrade.SULFURIC_ARC, 4)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SOLVENT_BYPRODUCT).get()),
                    new ArcFurnaceRecipe().solid(ore(type, BedrockOreGrade.SOLVENT_ARC, 2)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SOLVENT_ROASTED).get()),
                    new ArcFurnaceRecipe().solid(ore(type, BedrockOreGrade.SOLVENT_ARC, 4)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.RAD_BYPRODUCT).get()),
                    new ArcFurnaceRecipe().solid(ore(type, BedrockOreGrade.RAD_ARC, 2)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.RAD_ROASTED).get()),
                    new ArcFurnaceRecipe().solid(ore(type, BedrockOreGrade.RAD_ARC, 4)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_FIRST).get()),
                    new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 5), toFluid(type.primary2, 2)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_SECOND).get()),
                    new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 2), toFluid(type.primary2, 5)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.CRUMBS).get()),
                    new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 1), toFluid(type.primary2, 1)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SULFURIC_WASHED).get()),
                    new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductAcid1, 3), toFluid(type.byproductAcid2, 3), toFluid(type.byproductAcid3, 3)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SOLVENT_WASHED).get()),
                    new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductSolvent1, 3), toFluid(type.byproductSolvent2, 3), toFluid(type.byproductSolvent3, 3)));
            ArcFurnaceRecipes.register(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.RAD_WASHED).get()),
                    new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductRad1, 3), toFluid(type.byproductRad2, 3), toFluid(type.byproductRad3, 3)));
        }

        // CE ArcFurnaceRecipes.java:74-91 material × shape autogen
        for (NTMMaterial material : Mats.orderedList) {
            NTMMaterial convert = material.smeltsInto;
            if (convert.smeltable != NTMMaterial.SmeltingBehavior.SMELTABLE) continue;
            for (MaterialShapes shape : MaterialShapes.allShapes) {
                if (shape.noAutogen || shape.tagFolder == null) continue;
                ArcFurnaceRecipes.register(new OreDictStack(shape.commonTag(material)),
                        new ArcFurnaceRecipe().fluid(new Mats.MaterialStack(convert, shape.q(1) * material.convOut / material.convIn)));
            }
        }

        // CE ArcFurnaceRecipes.java:94-100 custom smeltables
        for (Map.Entry<String, List<Mats.MaterialStack>> entry : Mats.materialOreEntries.entrySet()) {
            addCustomSmeltable(new OreDictStack(c(entry.getKey())), entry.getValue());
        }
        for (Map.Entry<Item, List<Mats.MaterialStack>> entry : Mats.materialEntries.entrySet()) {
            addCustomSmeltable(new ComparableStack(entry.getKey()), entry.getValue());
        }
    }

    public static void register(AStack input, ArcFurnaceRecipe output) {
        List<ItemStack> inputs = input.extractForJEI();
        for (ItemStack stack : inputs) {
            ComparableStack comp = new ComparableStack(stack).makeSingular();
            if (output.solidOutput != null && occupiedSolid.contains(comp)) return;
            if (output.fluidOutput != null && occupiedLiquid.contains(comp)) return;
        }
        recipeList.add(new Tuple.Pair<>(input, output));
        for (ItemStack stack : inputs) {
            ComparableStack comp = new ComparableStack(stack).makeSingular();
            if (output.solidOutput != null) occupiedSolid.add(comp);
            if (output.fluidOutput != null) occupiedLiquid.add(comp);
        }
    }

    private static void addCustomSmeltable(AStack astack, List<Mats.MaterialStack> mats) {
        List<Mats.MaterialStack> smeltables = new ArrayList<>();
        for (Mats.MaterialStack mat : mats) {
            if (mat.material.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) smeltables.add(mat);
        }
        if (smeltables.isEmpty()) return;
        register(astack, new ArcFurnaceRecipe().fluid(smeltables.toArray(new Mats.MaterialStack[0])));
    }

    public static ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid) {
        register();
        if (stack == null || stack.isEmpty()) return null;
        ComparableStack cacheKey = new ComparableStack(stack).makeSingular();
        Map<ComparableStack, ArcFurnaceRecipe> cache = liquid ? fastCacheLiquid : fastCacheSolid;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
        for (Tuple.Pair<AStack, ArcFurnaceRecipe> entry : recipeList) {
            if (!entry.getKey().matchesRecipe(stack, true)) continue;
            ArcFurnaceRecipe rec = entry.getValue();
            if ((liquid && rec.fluidOutput != null) || (!liquid && rec.solidOutput != null)) {
                cache.put(cacheKey, rec);
                return rec;
            }
        }
        cache.put(cacheKey, null);
        return null;
    }

    /** CE {@code ItemBedrockOreNew.toFluid} :278-282. */
    private static Mats.MaterialStack toFluid(BedrockOreOutput o, double amount) {
        if (o != null && o.material() != null && o.material().smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return new Mats.MaterialStack(o.material(), (int) Math.ceil(MaterialShapes.FRAGMENT.q(o.amount()) * amount));
        }
        return null;
    }

    private static ItemStack ore(BedrockOreType type, BedrockOreGrade grade, int count) {
        return new ItemStack(BedrockOreItems.get(type, grade).get(), count);
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static class ArcFurnaceRecipe {
        public Mats.MaterialStack[] fluidOutput;
        public ItemStack solidOutput;

        public ArcFurnaceRecipe fluid(Mats.MaterialStack... outputs) {
            this.fluidOutput = outputs;
            return this;
        }

        public ArcFurnaceRecipe fluidNull(Mats.MaterialStack... outputs) {
            List<Mats.MaterialStack> mat = new ArrayList<>();
            for (Mats.MaterialStack stack : outputs) {
                if (stack != null) mat.add(stack);
            }
            if (!mat.isEmpty()) this.fluidOutput = mat.toArray(new Mats.MaterialStack[0]);
            return this;
        }

        public ArcFurnaceRecipe solid(ItemStack output) {
            this.solidOutput = output;
            return this;
        }
    }
}
