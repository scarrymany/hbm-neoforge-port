package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code CombinationRecipes.java}:86-134. Heat-driven combo oven table.
 * Each {@code RECIPES.put} is a census site. AIR outputs are dropped after register.
 */
public final class CombinationRecipes {

    public static final Map<AStack, CombinationRecipe> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CombinationRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE CombinationRecipes.java:86-88 coal
        RECIPES.put(new ComparableStack(Items.COAL), rec(stack("coke_coal"), new FluidStack(Fluids.COALCREOSOTE, 100)));
        RECIPES.put(new ComparableStack(item("powder_coal")), rec(stack("coke_coal"), new FluidStack(Fluids.COALCREOSOTE, 100)));
        RECIPES.put(new ComparableStack(item("briquette_coal")), rec(stack("coke_coal"), new FluidStack(Fluids.COALCREOSOTE, 150)));

        // :91-94 lignite
        RECIPES.put(new ComparableStack(item("lignite")), rec(stack("coke_lignite"), new FluidStack(Fluids.COALCREOSOTE, 50)));
        RECIPES.put(new ComparableStack(item("powder_lignite")), rec(stack("coke_lignite"), new FluidStack(Fluids.COALCREOSOTE, 50)));
        RECIPES.put(new ComparableStack(item("briquette_lignite")), rec(stack("coke_lignite"), new FluidStack(Fluids.COALCREOSOTE, 100)));

        // :96-104
        RECIPES.put(new ComparableStack(item("powder_chlorocalcite")), rec(stack("powder_calcium"), new FluidStack(Fluids.CHLORINE, 250)));
        RECIPES.put(new ComparableStack(item("powder_molysite")), rec(new ItemStack(Items.IRON_INGOT), new FluidStack(Fluids.CHLORINE, 250)));
        RECIPES.put(new ComparableStack(item("crystal_cinnabar")), rec(stack("sulfur"), new FluidStack(Fluids.MERCURY, 100)));
        RECIPES.put(new ComparableStack(Items.GLOWSTONE_DUST), rec(stack("sulfur"), new FluidStack(Fluids.CHLORINE, 100)));
        RECIPES.put(new ComparableStack(item("gem_sodalite")), rec(stack("powder_sodium"), new FluidStack(Fluids.CHLORINE, 100)));
        RECIPES.put(new ComparableStack(item("chunk_ore_cryolite")), rec(stack("powder_aluminium"), new FluidStack(Fluids.LYE, 150)));
        RECIPES.put(new ComparableStack(item("powder_sodium")), rec(ItemStack.EMPTY, new FluidStack(Fluids.SODIUM, 100)));
        RECIPES.put(new ComparableStack(item("powder_limestone")), rec(stack("powder_calcium"), new FluidStack(Fluids.CARBONDIOXIDE, 50)));

        // :106-109 wood
        RECIPES.put(new OreDictStack(ItemTags.LOGS), rec(new ItemStack(Items.CHARCOAL), new FluidStack(Fluids.WOODOIL, 250)));
        RECIPES.put(new OreDictStack(ItemTags.SAPLINGS), rec(stack("powder_ash_wood"), new FluidStack(Fluids.WOODOIL, 50)));
        RECIPES.put(new ComparableStack(item("briquette_wood")), rec(new ItemStack(Items.CHARCOAL), new FluidStack(Fluids.WOODOIL, 500)));

        // :111-118 tar → coke
        RECIPES.put(new ComparableStack(item("oil_tar_crude")), rec(stack("coke_petroleum"), null));
        RECIPES.put(new ComparableStack(item("oil_tar_crack")), rec(stack("coke_petroleum"), null));
        RECIPES.put(new ComparableStack(item("oil_tar_coal")), rec(stack("coke_coal"), null));
        RECIPES.put(new ComparableStack(item("oil_tar_wood")), rec(stack("coke_coal"), null));

        // :120-121
        RECIPES.put(new ComparableStack(Items.SUGAR_CANE), rec(new ItemStack(Items.SUGAR, 2), new FluidStack(Fluids.ETHANOL, 50)));
        RECIPES.put(new ComparableStack(Blocks.CLAY), rec(new ItemStack(Blocks.BRICKS), null));

        // :123-134 bedrock roast loop — 5 put sites, same as CE
        for (BedrockOreType type : BedrockOreType.VALUES) {
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.BASE)),
                    rec(new ItemStack(bedrock(type, BedrockOreGrade.BASE_ROASTED)), new FluidStack(Fluids.VITRIOL, 50)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.PRIMARY)),
                    rec(new ItemStack(bedrock(type, BedrockOreGrade.PRIMARY_ROASTED)), new FluidStack(Fluids.VITRIOL, 50)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.SULFURIC_BYPRODUCT)),
                    rec(new ItemStack(bedrock(type, BedrockOreGrade.SULFURIC_ROASTED)), new FluidStack(Fluids.VITRIOL, 50)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.SOLVENT_BYPRODUCT)),
                    rec(new ItemStack(bedrock(type, BedrockOreGrade.SOLVENT_ROASTED)), new FluidStack(Fluids.VITRIOL, 50)));
            RECIPES.put(new ComparableStack(bedrock(type, BedrockOreGrade.RAD_BYPRODUCT)),
                    rec(new ItemStack(bedrock(type, BedrockOreGrade.RAD_ROASTED)), new FluidStack(Fluids.VITRIOL, 50)));
        }

        RECIPES.entrySet().removeIf(e -> {
            ItemStack out = e.getValue().output;
            return out != null && !out.isEmpty() && out.getItem() == Items.AIR;
        });
    }

    public static CombinationRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        for (Map.Entry<AStack, CombinationRecipe> e : RECIPES.entrySet()) {
            if (e.getKey().matchesRecipe(stack, true)) return e.getValue();
        }
        return null;
    }

    private static CombinationRecipe rec(ItemStack out, FluidStack fluid) {
        return new CombinationRecipe(out, fluid);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
    }

    private static Item bedrock(BedrockOreType type, BedrockOreGrade grade) {
        return BedrockOreItems.get(type, grade).get();
    }

    public static final class CombinationRecipe {
        public final ItemStack output;
        public final FluidStack fluid;

        public CombinationRecipe(ItemStack output, FluidStack fluid) {
            this.output = output == null ? ItemStack.EMPTY : output;
            this.fluid = fluid;
        }
    }
}
