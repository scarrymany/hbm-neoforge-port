package com.hbm.inventory.recipes.crafting;

import com.hbm.items.weapon.ArtilleryAmmo;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * CE {@code CargoShellCraftingHandler}: shapeless empty {@code ammo_arty} meta 8 + one cargo item
 * (no container leftover) → shell NBT {@code cargo}.
 */
public final class CargoShellCraftingRecipe implements CraftingRecipe {

    public static final CargoShellCraftingRecipe INSTANCE = new CargoShellCraftingRecipe();

    private CargoShellCraftingRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return scan(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Scanned scanned = scan(input);
        if (scanned == null) return ItemStack.EMPTY;
        return ArtilleryAmmo.setCargo(scanned.shell, scanned.cargo, registries);
    }

    private static Scanned scan(CraftingInput input) {
        ItemStack shell = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;
        int items = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.hasCraftingRemainingItem()) return null;
            items++;
            if (isEmptyCargoShell(stack)) {
                if (!shell.isEmpty()) return null;
                shell = stack;
            } else {
                if (!cargo.isEmpty()) return null;
                cargo = stack;
            }
        }
        if (items != 2 || shell.isEmpty() || cargo.isEmpty()) return null;
        return new Scanned(shell, cargo);
    }

    /** CE: {@code ammo_arty} meta 8 and {@code !stack.hasTagCompound()}. */
    private static boolean isEmptyCargoShell(ItemStack stack) {
        if (ArtilleryAmmo.typeOfArty(stack.getItem()) != ArtilleryAmmo.ARTY_CARGO) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || !data.copyTag().contains("cargo");
    }

    private record Scanned(ItemStack shell, ItemStack cargo) {
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ArtilleryAmmo.item("ammo_arty_cargo"));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<CargoShellCraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static final class Serializer implements RecipeSerializer<CargoShellCraftingRecipe> {

        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<CargoShellCraftingRecipe> CODEC = MapCodec.unit(CargoShellCraftingRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, CargoShellCraftingRecipe> STREAM_CODEC =
                StreamCodec.unit(CargoShellCraftingRecipe.INSTANCE);

        @Override
        public MapCodec<CargoShellCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CargoShellCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
