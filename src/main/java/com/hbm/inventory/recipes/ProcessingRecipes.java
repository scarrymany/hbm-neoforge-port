package com.hbm.inventory.recipes;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link RecipeType}/{@link RecipeSerializer} registrations for the shredder and assembler (Phase 2's
 * {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md} package). Crystallizer and
 * mixer recipes are NOT JSON {@code Recipe<?>} types - see {@link CrystallizerRecipes}/
 * {@link MixerRecipes}'s own class javadoc for why those two stay bespoke plain-Java data classes
 * (their real shape - a required-fluid-type map key for the crystallizer, a competing-recipe-array
 * per output fluid for the mixer - doesn't fit vanilla's {@code Recipe<RecipeInput>} contract without
 * inventing a much larger custom-ingredient system than this task's scope calls for; this follows the
 * exact precedent the concurrent oil-production-chain pass's {@code RefineryRecipes} already set for
 * this same reason).
 * <p>
 * <b>Registration mechanics - deliberately NOT a new {@code DeferredRegister}</b>: this class appends
 * directly to {@link HbmRecipes#RECIPE_TYPES}/{@link HbmRecipes#RECIPE_SERIALIZERS} (both public
 * static fields, exactly the "any class may append" contract {@code ModBlocks.BLOCKS}/
 * {@code BLOCK_ENTITY_TYPES} and {@code ModMenuTypes.MENU_TYPES} already rely on elsewhere in this
 * port - see {@code PowerGenMenus}'s own javadoc for the precedent this follows) rather than owning a
 * second {@code DeferredRegister<RecipeType<?>>} pair that would need its own
 * {@code register(modEventBus)} call wired in from {@code MainRegistry} (a shared file this task was
 * told not to race on editing, same concern as {@code ModBlocks}/{@code ModItems}). Since
 * {@code HbmRecipes.register(modEventBus)} already fires {@code RECIPE_TYPES.register(modEventBus)}/
 * {@code RECIPE_SERIALIZERS.register(modEventBus)}, the two entries below only need this class to be
 * <i>loaded</i> (static initializers run) before that call - guaranteed by {@link #bootstrap()} being
 * invoked from {@code ProcessingBlocks.registerAll()}, itself called from {@code ModBlocks.register()}
 * strictly before {@code HbmRecipes.register(modEventBus)} in {@code MainRegistry}'s constructor (see
 * this task's wiring notes).
 */
public final class ProcessingRecipes {

    private ProcessingRecipes() {
    }

    /**
     * Shredder recipes reuse {@link HbmSimpleRecipe} directly (ingredient -&gt; output + duration -
     * exactly its shape, see that class's own javadoc inviting this). This is the first real second
     * consumer of that class, which surfaced a genuine bug in it - {@code getType()}/
     * {@code getSerializer()} were hardcoded to {@link HbmRecipes#SIMPLE_TYPE}/{@code SIMPLE_SERIALIZER}
     * on every instance, so a shredder recipe decoded via a distinct {@code "hbm:shredder"} serializer
     * would still report itself as {@code hbm:hbm_simple} and never appear in
     * {@code RecipeManager.getAllRecipesFor(SHREDDER_TYPE)}. Fixed narrowly in {@link HbmSimpleRecipe}
     * itself (see that class's own javadoc "Bug fix" section) rather than worked around here - the JSON
     * shape and every existing field are unchanged.
     */
    public static final DeferredHolder<RecipeType<?>, RecipeType<HbmSimpleRecipe>> SHREDDER_TYPE =
            HbmRecipes.RECIPE_TYPES.register("shredder", () -> new RecipeType<HbmSimpleRecipe>() {
                @Override
                public String toString() {
                    return "hbm:shredder";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HbmSimpleRecipe>> SHREDDER_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("shredder", () -> new HbmSimpleRecipe.Serializer(SHREDDER_TYPE));

    /** Assembler recipes use their own multi-input {@link AssemblerRecipe} shape - see that class's javadoc. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblerRecipe>> ASSEMBLER_TYPE =
            HbmRecipes.RECIPE_TYPES.register("assembler", () -> new RecipeType<AssemblerRecipe>() {
                @Override
                public String toString() {
                    return "hbm:assembler";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblerRecipe>> ASSEMBLER_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("assembler", () -> AssemblerRecipe.Serializer.INSTANCE);

    /** No-op - see class javadoc. Forces this class's static fields (and therefore the registrations above) to run. */
    public static void bootstrap() {
    }
}
