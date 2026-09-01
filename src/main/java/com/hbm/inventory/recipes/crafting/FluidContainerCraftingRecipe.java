package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
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
 * CE {@code CraftingManager} fluid-meta crafts. Port cells/tanks are one item + component, so
 * vanilla JSON {@code {"item":"hbm:cell"}} would match empty. This is the 1.21 match of
 * {@code new ItemStack(cell, 1, Fluids.AMAT.getID())} — same four CE rows, no invented I/O.
 * <ul>
 *   <li>{@code :614} 3×3 AMAT cell → {@code pellet_antimatter}</li>
 *   <li>{@code :642} euphemium + AMAT cell + singularity → {@code ams_core_sing}</li>
 *   <li>{@code :644} dalekanium + lava barrel + black_hole → {@code ams_core_eyeofharmony}</li>
 *   <li>{@code :491} barbed_wire + peroxide tank → {@code barbed_wire_acid} ×8</li>
 * </ul>
 */
public final class FluidContainerCraftingRecipe implements CraftingRecipe {

    public static final FluidContainerCraftingRecipe INSTANCE = new FluidContainerCraftingRecipe();

    private FluidContainerCraftingRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return kind(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Kind k = kind(input);
        return k == null ? ItemStack.EMPTY : k.result();
    }

    private static Kind kind(CraftingInput in) {
        if (in.width() < 3 || in.height() < 3) return null;
        if (amatNine(in)) return Kind.PELLET;
        if (itemAt(in, 1, 1, "singularity")
                && cellAt(in, 1, 0, Fluids.AMAT) && cellAt(in, 0, 1, Fluids.AMAT)
                && cellAt(in, 2, 1, Fluids.AMAT) && cellAt(in, 1, 2, Fluids.AMAT)
                && itemAt(in, 0, 0, "plate_euphemium") && itemAt(in, 2, 0, "plate_euphemium")
                && itemAt(in, 0, 2, "plate_euphemium") && itemAt(in, 2, 2, "plate_euphemium")) {
            return Kind.SING;
        }
        if (itemAt(in, 1, 1, "black_hole")
                && tankAt(in, 1, 0, "fluid_barrel_full", Fluids.LAVA, 16000)
                && tankAt(in, 0, 1, "fluid_barrel_full", Fluids.LAVA, 16000)
                && tankAt(in, 2, 1, "fluid_barrel_full", Fluids.LAVA, 16000)
                && tankAt(in, 1, 2, "fluid_barrel_full", Fluids.LAVA, 16000)
                && itemAt(in, 0, 0, "plate_dalekanium") && itemAt(in, 2, 0, "plate_dalekanium")
                && itemAt(in, 0, 2, "plate_dalekanium") && itemAt(in, 2, 2, "plate_dalekanium")) {
            return Kind.EYE;
        }
        if (itemAt(in, 0, 0, "barbed_wire")
                && tankAt(in, 1, 1, "fluid_tank_full", Fluids.PEROXIDE, 1000)
                && itemAt(in, 1, 0, "barbed_wire") && itemAt(in, 2, 0, "barbed_wire")
                && itemAt(in, 0, 1, "barbed_wire") && itemAt(in, 2, 1, "barbed_wire")
                && itemAt(in, 0, 2, "barbed_wire") && itemAt(in, 1, 2, "barbed_wire")
                && itemAt(in, 2, 2, "barbed_wire")) {
            return Kind.ACID;
        }
        return null;
    }

    private static boolean amatNine(CraftingInput in) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (!cellAt(in, x, y, Fluids.AMAT)) return false;
            }
        }
        return true;
    }

    private static boolean cellAt(CraftingInput in, int x, int y, FluidType type) {
        return ItemCell.hasFluid(at(in, x, y), type);
    }

    private static boolean tankAt(CraftingInput in, int x, int y, String id, FluidType type, int amount) {
        Item want = item(id);
        if (want == Items.AIR) return false;
        ItemStack stack = at(in, x, y);
        if (stack.getItem() != want) return false;
        if (!(stack.getItem() instanceof ItemFluidTank tank)) return false;
        return ItemFluidTank.getFluidType(stack) == type && tank.getFill(stack) >= amount;
    }

    private static boolean itemAt(CraftingInput in, int x, int y, String id) {
        Item want = item(id);
        return want != Items.AIR && at(in, x, y).getItem() == want;
    }

    private static ItemStack at(CraftingInput in, int x, int y) {
        return in.getItem(x + y * in.width());
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id, int n) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }

    private enum Kind {
        PELLET,
        SING,
        EYE,
        ACID;

        ItemStack result() {
            return switch (this) {
                case PELLET -> stack("pellet_antimatter", 1);
                case SING -> stack("ams_core_sing", 1);
                case EYE -> stack("ams_core_eyeofharmony", 1);
                case ACID -> stack("barbed_wire_acid", 8);
            };
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return stack("pellet_antimatter", 1);
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
    public RecipeSerializer<FluidContainerCraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static final class Serializer implements RecipeSerializer<FluidContainerCraftingRecipe> {

        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<FluidContainerCraftingRecipe> CODEC =
                MapCodec.unit(FluidContainerCraftingRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, FluidContainerCraftingRecipe> STREAM_CODEC =
                StreamCodec.unit(FluidContainerCraftingRecipe.INSTANCE);

        @Override
        public MapCodec<FluidContainerCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidContainerCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
