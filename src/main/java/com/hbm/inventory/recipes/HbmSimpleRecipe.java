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

import java.util.function.Supplier;

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
 * machine's own recipes) — see {@link HbmRecipes} for the one demonstration registration and
 * {@code com.hbm.inventory.recipes.ProcessingRecipes} (Phase 2's shredder/assembler package) for the
 * first real second consumer.
 * <p>
 * <b>Bug fix (Phase 2 machines pass) — {@code getType()}/{@code getSerializer()} used to be
 * hardcoded</b>: the original version of this class unconditionally returned
 * {@code HbmRecipes.SIMPLE_TYPE.get()}/{@code HbmRecipes.SIMPLE_SERIALIZER.get()} from every
 * instance, which silently broke the class's own "multiple machines can register their own
 * RecipeType/RecipeSerializer pair using THIS class directly" promise: {@link net.minecraft.world.item.crafting.RecipeManager}
 * buckets a loaded recipe by its own {@code getType()}, not by which {@code RecipeSerializer}
 * decoded it — so a second machine's recipes, decoded via a distinct serializer registered under a
 * distinct id (e.g. {@code "hbm:shredder"}), would still report {@code getType() == hbm:hbm_simple}
 * and therefore never show up in that machine's own {@code getAllRecipesFor(SHREDDER_TYPE)} lookup.
 * Fixed narrowly, without changing the JSON shape or the {@code group}/{@code input}/{@code output}/
 * {@code duration} fields any existing consumer depends on: each {@link Serializer} instance now
 * carries the {@link RecipeType} supplier it was built for (defaulting to {@link HbmRecipes#SIMPLE_TYPE}
 * for the existing demo registration/the public 4-arg constructor, so that call site is unaffected),
 * and decodes {@link HbmSimpleRecipe} instances that close over both that type supplier and the
 * decoding {@link Serializer} instance itself (no separate serializer-supplier param needed — a
 * {@code Serializer} already knows what "itself" is, so no static self-reference is required to wire
 * a second machine's {@code RecipeType}/{@code RecipeSerializer} pair together, see
 * {@code ProcessingRecipes}' own registration for the pattern a new machine family should follow:
 * {@code new HbmSimpleRecipe.Serializer(MY_TYPE)}, registered under its own serializer id).
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
    private final Supplier<RecipeType<HbmSimpleRecipe>> typeSupplier;
    private final RecipeSerializer<HbmSimpleRecipe> serializer;

    /** Public convenience constructor for {@link HbmRecipes}' own demonstration {@code hbm_simple} type/serializer - unaffected by the bug fix above. */
    public HbmSimpleRecipe(String group, Ingredient input, ItemStack output, int duration) {
        this(HbmRecipes.SIMPLE_TYPE, HbmSimpleRecipe.Serializer.INSTANCE, group, input, output, duration);
    }

    /** Used internally by {@link Serializer#codec()}/{@link Serializer#streamCodec()} - a decoded recipe remembers which (type, serializer) pair produced it. */
    HbmSimpleRecipe(Supplier<RecipeType<HbmSimpleRecipe>> typeSupplier, RecipeSerializer<HbmSimpleRecipe> serializer,
                    String group, Ingredient input, ItemStack output, int duration) {
        this.typeSupplier = typeSupplier;
        this.serializer = serializer;
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
        return typeSupplier.get();
    }

    @Override
    public RecipeSerializer<HbmSimpleRecipe> getSerializer() {
        return serializer;
    }

    public static class Serializer implements RecipeSerializer<HbmSimpleRecipe> {

        /** {@link HbmRecipes}' own demonstration {@code hbm_simple} serializer - unaffected by the bug fix above. */
        public static final Serializer INSTANCE = new Serializer(HbmRecipes.SIMPLE_TYPE);

        private final MapCodec<HbmSimpleRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, HbmSimpleRecipe> streamCodec;

        /**
         * @param typeSupplier the {@link RecipeType} every {@link HbmSimpleRecipe} this serializer
         *                     decodes should report from {@link HbmSimpleRecipe#getType()} - pass
         *                     your machine's own {@code RecipeType<HbmSimpleRecipe>} field (a
         *                     {@link net.neoforged.neoforge.registries.DeferredHolder} already
         *                     implements {@link Supplier}, so it can be passed directly).
         */
        public Serializer(Supplier<RecipeType<HbmSimpleRecipe>> typeSupplier) {
            this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(HbmSimpleRecipe::getGroup),
                    Ingredient.CODEC.fieldOf("input").forGetter(r -> r.input),
                    ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                    Codec.INT.optionalFieldOf("duration", 0).forGetter(HbmSimpleRecipe::getDuration)
            ).apply(instance, (g, i, o, d) -> new HbmSimpleRecipe(typeSupplier, this, g, i, o, d)));

            this.streamCodec = StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, HbmSimpleRecipe::getGroup,
                    Ingredient.CONTENTS_STREAM_CODEC, HbmSimpleRecipe::getInput,
                    ItemStack.STREAM_CODEC, r -> r.output,
                    ByteBufCodecs.VAR_INT, HbmSimpleRecipe::getDuration,
                    (g, i, o, d) -> new HbmSimpleRecipe(typeSupplier, this, g, i, o, d)
            );
        }

        @Override
        public MapCodec<HbmSimpleRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmSimpleRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
