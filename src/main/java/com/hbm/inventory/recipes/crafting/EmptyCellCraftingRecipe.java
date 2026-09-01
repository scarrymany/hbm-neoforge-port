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
 * CE empty-cell table crafts. Vanilla {@code {"item":"hbm:cell"}} matches filled cells too;
 * CE uses {@code new ItemStack(ModItems.cell)} = meta 0 only.
 * <ul>
 *   <li>{@code RodRecipes.java:91} empty cell + {@code lithium} → {@code pile_rod_lithium}</li>
 *   <li>{@code ConsumableRecipes.java:96} {@code P}/{@code C}/{@code B} plate_iron + empty cell
 *       + iron bars → {@code syringe_empty} ×6</li>
 *   <li>{@code CraftingManager.java:172} 8 empty cells + {@code mike_deut} → 8 deuterium cells</li>
 * </ul>
 * Tritium unload is {@link BreedingRodTritiumCellRecipe}. No invented I/O.
 */
public final class EmptyCellCraftingRecipe implements CraftingRecipe {

    public static final EmptyCellCraftingRecipe INSTANCE = new EmptyCellCraftingRecipe();

    private EmptyCellCraftingRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return kind(input) != Kind.NONE;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return switch (kind(input)) {
            case LITHIUM -> stack("pile_rod_lithium", 1);
            case SYRINGE -> stack("syringe_empty", 6);
            case DEUTERIUM -> ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.DEUTERIUM, 8);
            case NONE -> ItemStack.EMPTY;
        };
    }

    private static Kind kind(CraftingInput in) {
        if (lithium(in)) {
            return Kind.LITHIUM;
        }
        if (syringe(in)) {
            return Kind.SYRINGE;
        }
        if (deuterium(in)) {
            return Kind.DEUTERIUM;
        }
        return Kind.NONE;
    }

    /** Shapeless empty cell + {@code lithium}. */
    private static boolean lithium(CraftingInput in) {
        int n = 0;
        boolean cell = false;
        boolean li = false;
        for (int i = 0; i < in.size(); i++) {
            ItemStack stack = in.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            n++;
            if (ItemCell.isEmptyCell(stack)) {
                cell = true;
            } else if (stack.getItem() == item("lithium")) {
                li = true;
            } else {
                return false;
            }
        }
        return n == 2 && cell && li;
    }

    /** Shaped column {@code P}/{@code C}/{@code B}. */
    private static boolean syringe(CraftingInput in) {
        int w = in.width();
        int h = in.height();
        if (h < 3) {
            return false;
        }
        int found = 0;
        for (int x = 0; x < w; x++) {
            boolean match = isItem(at(in, x, 0), "plate_iron")
                    && ItemCell.isEmptyCell(at(in, x, 1))
                    && at(in, x, 2).is(Items.IRON_BARS);
            if (h > 3) {
                for (int y = 3; y < h; y++) {
                    if (!at(in, x, y).isEmpty()) {
                        match = false;
                    }
                }
            }
            if (match) {
                found++;
            } else {
                for (int y = 0; y < h; y++) {
                    if (!at(in, x, y).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return found == 1;
    }

    /** {@code DDD}/{@code DTD}/{@code DDD}, D=empty cell, T=mike_deut. */
    private static boolean deuterium(CraftingInput in) {
        if (in.width() < 3 || in.height() < 3) {
            return false;
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack stack = at(in, x, y);
                if (x == 1 && y == 1) {
                    if (stack.getItem() != item("mike_deut")) {
                        return false;
                    }
                } else if (!ItemCell.isEmptyCell(stack)) {
                    return false;
                }
            }
        }
        for (int i = 0; i < in.size(); i++) {
            int x = i % in.width();
            int y = i / in.width();
            if (x < 3 && y < 3) {
                continue;
            }
            if (!in.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack at(CraftingInput in, int x, int y) {
        return in.getItem(x + y * in.width());
    }

    private static boolean isItem(ItemStack stack, String id) {
        Item want = item(id);
        return want != Items.AIR && stack.getItem() == want;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id, int n) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }

    private enum Kind {
        NONE,
        LITHIUM,
        SYRINGE,
        DEUTERIUM
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return stack("pile_rod_lithium", 1);
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
    public RecipeSerializer<EmptyCellCraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static final class Serializer implements RecipeSerializer<EmptyCellCraftingRecipe> {

        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<EmptyCellCraftingRecipe> CODEC =
                MapCodec.unit(EmptyCellCraftingRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, EmptyCellCraftingRecipe> STREAM_CODEC =
                StreamCodec.unit(EmptyCellCraftingRecipe.INSTANCE);

        @Override
        public MapCodec<EmptyCellCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EmptyCellCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
