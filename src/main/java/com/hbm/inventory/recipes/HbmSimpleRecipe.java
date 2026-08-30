package com.hbm.inventory.recipes;

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
 * Reusable JSON-{@code Recipe<?>} shape for the "classic" single-input/single-output machine
 * recipes docs/phase2/items_tool_machine_coupling_and_recipe_system.md's Part B design calls tier
 * 1 ({@code ShredderRecipes}, {@code CentrifugeRecipes}, {@code PressRecipes}, etc — the majority of
 * CE's ~60 recipe classes): one {@link Ingredient} in, one {@link ItemStack} out, an optional
 * processing {@code duration} in ticks. Duration/power for a given machine were hardcoded TE
 * constants in CE, not per-recipe fields (confirmed by
 * {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s shredder analysis) — the
 * {@code duration} field here is this scaffolding's own addition for machines that DO want a
 * per-recipe duration, optional (defaults to 0) so a machine that doesn't need it can just ignore
 * it.
 * <p>
 * This is deliberately generic/reusable, not a specific machine's recipe — see
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s design point 1: "one
 * shared {@code HbmSimpleRecipe} ... per machine family, JSON shape
 * {@code {"input": [...], "output": [...]}}". Multiple machines can register their own
 * {@link RecipeType}/{@link RecipeSerializer} pair using THIS class directly (share the shape,
 * separate type/serializer per machine so `RecipeManager.getAllRecipesFor(type)` only returns that
 * machine's own recipes) — see {@link HbmRecipes} for the one demonstration registration. A machine
 * needing more than one input, a fluid, or chance-weighted outputs needs its own
 * {@code Recipe<?>} type following this same pattern, not a reused {@code HbmSimpleRecipe}.
 * <p>
 * <b>Unverified against a real NeoForge 1.21.1 build</b> (this sandbox has no NeoForge jar/network
 * access, see docs/phase2 Open Questions — same constraint that report's own design already flagged
 * for this exact area): the {@link Recipe}/{@link RecipeSerializer} abstract-method set below, and
 * the {@code Ingredient.CODEC}/{@code Ingredient.CONTENTS_STREAM_CODEC}/{@code ItemStack.CODEC}/
 * {@code ItemStack.STREAM_CODEC} static field names, are written from well-established 1.20.5+
 * vanilla recipe-codec conventions (the same shape {@code SmeltingRecipe}/
 * {@code SimpleCookingSerializer} use), not confirmed by compiling. Whoever next has build access
 * should verify this file compiles before relying on it.
 */
public class HbmSimpleRecipe implements Recipe<SingleRecipeInput> {

    protected final String group;
    protected final Ingredient input;
    protected final ItemStack output;
    protected final int duration;

    public HbmSimpleRecipe(String group, Ingredient input, ItemStack output, int duration) {
        this.group = group;
        this.input = input;
        this.output = output;
        this.duration = duration;
    }

    public Ingredient getInput() {
        return input;
    }

    public int getDuration() {
        return duration;
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
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipes.SIMPLE_TYPE.get();
    }

    @Override
    public RecipeSerializer<HbmSimpleRecipe> getSerializer() {
        return HbmRecipes.SIMPLE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<HbmSimpleRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<HbmSimpleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(HbmSimpleRecipe::getGroup),
                Ingredient.CODEC.fieldOf("input").forGetter(r -> r.input),
                ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                Codec.INT.optionalFieldOf("duration", 0).forGetter(HbmSimpleRecipe::getDuration)
        ).apply(instance, HbmSimpleRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, HbmSimpleRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, HbmSimpleRecipe::getGroup,
                Ingredient.CONTENTS_STREAM_CODEC, HbmSimpleRecipe::getInput,
                ItemStack.STREAM_CODEC, r -> r.output,
                ByteBufCodecs.VAR_INT, HbmSimpleRecipe::getDuration,
                HbmSimpleRecipe::new
        );

        @Override
        public MapCodec<HbmSimpleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmSimpleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
