package com.hbm.inventory.recipes.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * JSON {@code Recipe<?>} shape for {@link com.hbm.blockentity.machine.MachineReactorBreedingBlockEntity}'s
 * rod transmutation table, ported from CE's {@code com.hbm.inventory.recipes.BreederRecipes}
 * ({@code BreederRecipe} inner class: {@code ItemStack output; int flux;}, read in full).
 *
 * <p>Deliberately its own small class rather than reusing {@link com.hbm.inventory.recipes.HbmSimpleRecipe}
 * directly: that class's own extra numeric field is named/documented as a processing
 * {@code duration} (ticks) - CE's breeder recipe extra field is a flux <i>cost</i>, a different unit
 * with different call-site semantics (compared against the machine's per-tick flux income, not
 * counted down as a timer), so reusing the field under a misleading name would read wrong to the next
 * person touching either machine family. Same one-ingredient/one-output/one-extra-int shape
 * otherwise, registered through the same {@code RecipeType}/{@code RecipeSerializer}
 * {@link com.hbm.inventory.recipes.HbmRecipes}-append convention {@code ProcessingRecipes} already
 * established for a second {@link com.hbm.inventory.recipes.HbmSimpleRecipe} consumer - see
 * {@link BreederRecipes} for the registration and this file's own JSON shape:
 * {@code {"input": [...], "output": [...], "flux": n}}, one file per rod-family/multiplicity pair
 * under {@code data/hbm/recipe/breeder/} (30 entries: 10 base transmutations x
 * single/dual/quad, ported from CE's {@code BreederRecipes.registerDefaults()}'s ten
 * {@code setRecipe(...)} calls). CE's eleventh entry
 * ({@code meteorite_sword_etched -> meteorite_sword_bred}, 1000 flux) is not ported - neither item
 * exists anywhere in this port yet (grepped, zero hits); a genuinely out-of-scope weapon/tool item,
 * not a data-porting gap.
 */
public class BreederRecipe implements Recipe<SingleRecipeInput> {

    private final Ingredient input;
    private final ItemStack output;
    private final int flux;

    public BreederRecipe(Ingredient input, ItemStack output, int flux) {
        this.input = input;
        this.output = output;
        this.flux = flux;
    }

    public Ingredient getInput() {
        return input;
    }

    /** Flux cost this recipe needs banked (compared against {@code MachineReactorBreedingBlockEntity.flux} per tick), not a tick duration. */
    public int getFlux() {
        return flux;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.input);
        return list;
    }

    @Override
    public RecipeType<?> getType() {
        return BreederRecipes.BREEDER_TYPE.get();
    }

    @Override
    public RecipeSerializer<BreederRecipe> getSerializer() {
        return BreederRecipes.BREEDER_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<BreederRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<BreederRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(BreederRecipe::getInput),
                ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                Codec.INT.fieldOf("flux").forGetter(BreederRecipe::getFlux)
        ).apply(instance, BreederRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BreederRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, BreederRecipe::getInput,
                ItemStack.STREAM_CODEC, r -> r.output,
                ByteBufCodecs.VAR_INT, BreederRecipe::getFlux,
                BreederRecipe::new
        );

        @Override
        public MapCodec<BreederRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BreederRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
