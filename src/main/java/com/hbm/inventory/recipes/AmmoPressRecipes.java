package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
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
 * CE {@code AmmoPressRecipes.java}: 9-slot positional grid. Generated from CE registerDefaults.
 */
public final class AmmoPressRecipes {

    public static final List<AmmoPressRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private AmmoPressRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m357_bp"), 16),
                null, ((new ComparableStack(item("ingot_lead"), 1)).copy(2)), null, null, new ComparableStack(Items.GUNPOWDER), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m357_sp"), 8),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m357_fmj"), 8),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m357_jhp"), 8),
                OreDictStack.ofHbmTag("any_plastic", 1), new ComparableStack(item("ingot_copper"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m357_ap"), 8),
                null, new ComparableStack(item("ingot_weaponsteel"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_small_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m357_express"), 8),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(3)), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m44_bp"), 12),
                null, ((new ComparableStack(item("ingot_lead"), 1)).copy(2)), null, null, new ComparableStack(Items.GUNPOWDER), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m44_sp"), 6),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m44_fmj"), 6),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m44_jhp"), 6),
                OreDictStack.ofHbmTag("any_plastic", 1), new ComparableStack(item("ingot_copper"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m44_ap"), 6),
                null, new ComparableStack(item("ingot_weaponsteel"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_small_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("m44_express"), 6),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(3)), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p22_sp"), 24),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p22_fmj"), 24),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p22_jhp"), 24),
                OreDictStack.ofHbmTag("any_plastic", 1), new ComparableStack(item("ingot_copper"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p22_ap"), 24),
                null, new ComparableStack(item("ingot_weaponsteel"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_small_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p9_sp"), 12),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p9_fmj"), 12),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p9_jhp"), 12),
                OreDictStack.ofHbmTag("any_plastic", 1), new ComparableStack(item("ingot_copper"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_small")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("p9_ap"), 12),
                null, new ComparableStack(item("ingot_weaponsteel"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_small_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r556_sp"), 16),
                null, ((new ComparableStack(item("ingot_lead"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, ((new ComparableStack(item("casing_small"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r556_fmj"), 16),
                null, ((new ComparableStack(item("ingot_steel"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, ((new ComparableStack(item("casing_small"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r556_jhp"), 16),
                OreDictStack.ofHbmTag("any_plastic", 1), ((new ComparableStack(item("ingot_copper"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, ((new ComparableStack(item("casing_small"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r556_ap"), 16),
                null, ((new ComparableStack(item("ingot_weaponsteel"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(4)), null, null, ((new ComparableStack(item("casing_small_steel"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r762_sp"), 12),
                null, ((new ComparableStack(item("ingot_lead"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, ((new ComparableStack(item("casing_small"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r762_fmj"), 12),
                null, ((new ComparableStack(item("ingot_steel"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, ((new ComparableStack(item("casing_small"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r762_jhp"), 12),
                OreDictStack.ofHbmTag("any_plastic", 1), ((new ComparableStack(item("ingot_copper"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, ((new ComparableStack(item("casing_small"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r762_ap"), 12),
                null, ((new ComparableStack(item("ingot_weaponsteel"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(4)), null, null, ((new ComparableStack(item("casing_small_steel"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r762_du"), 12),
                null, ((new ComparableStack(item("ingot_u238"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(4)), null, null, ((new ComparableStack(item("casing_small_steel"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("r762_he"), 12),
                OreDictStack.ofHbmTag("any_highexplosive", 1), new ComparableStack(item("ingot_ferrouranium"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(4)), null, null, ((new ComparableStack(item("casing_small_steel"))).copy(2)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("bmg50_sp"), 12),
                null, ((new ComparableStack(item("ingot_lead"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(3)), null, null, new ComparableStack(item("casing_large")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("bmg50_fmj"), 12),
                null, ((new ComparableStack(item("ingot_steel"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(3)), null, null, new ComparableStack(item("casing_large")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("bmg50_jhp"), 12),
                OreDictStack.ofHbmTag("any_plastic", 1), ((new ComparableStack(item("ingot_copper"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(3)), null, null, new ComparableStack(item("casing_large")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("bmg50_ap"), 12),
                null, ((new ComparableStack(item("ingot_weaponsteel"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(6)), null, null, new ComparableStack(item("casing_large_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("bmg50_du"), 12),
                null, ((new ComparableStack(item("ingot_u238"), 1)).copy(2)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(6)), null, null, new ComparableStack(item("casing_large_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("bmg50_he"), 12),
                OreDictStack.ofHbmTag("any_highexplosive", 1), new ComparableStack(item("ingot_ferrouranium"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(6)), null, null, new ComparableStack(item("casing_large_steel")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_bp"), 6),
                null, ((new ComparableStack(item("nugget_lead"), 1)).copy(6)), null, null, new ComparableStack(Items.GUNPOWDER), null, null, new ComparableStack(item("casing_shotshell")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_bp_magnum"), 6),
                null, ((new ComparableStack(item("nugget_lead"), 1)).copy(8)), null, null, new ComparableStack(Items.GUNPOWDER), null, null, new ComparableStack(item("casing_shotshell")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_bp_slug"), 6),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, new ComparableStack(Items.GUNPOWDER), null, null, new ComparableStack(item("casing_shotshell")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12"), 6),
                null, ((new ComparableStack(item("nugget_lead"), 1)).copy(6)), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_buckshot")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_slug"), 6),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_buckshot")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_flechette"), 6),
                null, ((new ComparableStack(item("lead_bolt"), 1)).copy(12)), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_buckshot")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_magnum"), 6),
                null, ((new ComparableStack(item("nugget_lead"), 1)).copy(8)), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g12_explosive"), 6),
                null, OreDictStack.ofHbmTag("any_highexplosive", 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g10"), 4),
                null, ((new ComparableStack(item("nugget_lead"), 1)).copy(8)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g10_shrapnel"), 4),
                OreDictStack.ofHbmTag("any_plastic", 1), ((new ComparableStack(item("nugget_lead"), 1)).copy(8)), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g10_du"), 4),
                null, new ComparableStack(item("ingot_u238"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g10_slug"), 4),
                null, new ComparableStack(item("ingot_lead"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g10_explosive"), 4),
                OreDictStack.ofHbmTag("any_highexplosive", 1), new ComparableStack(item("ingot_ferrouranium"), 1), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(2)), null, null, new ComparableStack(item("casing_buckshot_advanced")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("g40_demo"), 4),
                null, ((OreDictStack.ofHbmTag("any_highexplosive", 1)).copy(2)), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null, null, new ComparableStack(item("casing_large")), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("rocket_demo"), 2),
                null, ((OreDictStack.ofHbmTag("any_highexplosive", 1)).copy(2)), null, null, new ComparableStack(item("casing_large")), null, null, ((OreDictStack.ofHbmTag("any_smokeless", 1)).copy(3)), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("capacitor_ir"), 4),
                null, OreDictStack.ofHbmTag("any_plastic", 1), null, null, new ComparableStack(item("ingot_niobium"), 1), null, null, OreDictStack.ofHbmTag("any_plastic", 1), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("coil_ferrouranium"), 4),
                null, null, null, null, new ComparableStack(item("ingot_ferrouranium"), 1), null, null, null, null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("ct_hook"), 16),
                null, new ComparableStack(item("ingot_steel"), 1), null, null, new ComparableStack(item("steel_pipe"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null));
        RECIPES.add(new AmmoPressRecipe(new ItemStack(item("ct_mortar"), 4),
                null, ((OreDictStack.ofHbmTag("any_highexplosive", 1)).copy(4)), null, null, new ComparableStack(item("steel_pipe"), 1), null, null, OreDictStack.ofHbmTag("any_smokeless", 1), null));
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

    public static List<AmmoPressRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    public static class AmmoPressRecipe {
        public final ItemStack output;
        public final Object[] slots;

        public AmmoPressRecipe(ItemStack output, Object... slots) {
            this.output = output;
            this.slots = slots;
        }
    }
}
