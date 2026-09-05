package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.SpecialItems;
import com.hbm.main.MainRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * CE {@code RodRecipes.java:61-63} tritium breeding-rod unload → filled tritium cell.
 * Vanilla JSON {@code {"item":"hbm:cell"}} matches empty; result must carry
 * {@code CELL_FLUID_ID}. Same three CE rows, no invented I/O.
 */
public final class BreedingRodTritiumCellRecipe implements CraftingRecipe {

    public static final BreedingRodTritiumCellRecipe INSTANCE = new BreedingRodTritiumCellRecipe();

    private BreedingRodTritiumCellRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return kind(input) > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int n = kind(input);
        return n <= 0 ? ItemStack.EMPTY : ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.TRITIUM, n);
    }

    /** 1 / 2 / 4 = CE single / dual / quad unload count; 0 = no match. */
    private static int kind(CraftingInput in) {
        int cells = 0;
        int single = 0;
        int dual = 0;
        int quad = 0;
        for (int i = 0; i < in.size(); i++) {
            ItemStack stack = in.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (ItemCell.isEmptyCell(stack)) {
                cells++;
                continue;
            }
            Item item = stack.getItem();
            if (item == item("rod_tritium")) {
                single++;
            } else if (item == item("rod_dual_tritium")) {
                dual++;
            } else if (item == item("rod_quad_tritium")) {
                quad++;
            } else {
                return 0;
            }
        }
        if (single == 1 && dual == 0 && quad == 0 && cells == 1) {
            return 1;
        }
        if (dual == 1 && single == 0 && quad == 0 && cells == 2) {
            return 2;
        }
        if (quad == 1 && single == 0 && dual == 0 && cells == 4) {
            return 4;
        }
        return 0;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        Item cell = item("cell");
        return cell == Items.AIR ? ItemStack.EMPTY : new ItemStack(cell);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<BreedingRodTritiumCellRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static final class Serializer implements RecipeSerializer<BreedingRodTritiumCellRecipe> {

        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<BreedingRodTritiumCellRecipe> CODEC =
                MapCodec.unit(BreedingRodTritiumCellRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, BreedingRodTritiumCellRecipe> STREAM_CODEC =
                StreamCodec.unit(BreedingRodTritiumCellRecipe.INSTANCE);

        @Override
        public MapCodec<BreedingRodTritiumCellRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BreedingRodTritiumCellRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
