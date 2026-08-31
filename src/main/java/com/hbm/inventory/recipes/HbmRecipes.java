package com.hbm.inventory.recipes;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration scaffolding for this port's JSON-datapack machine recipes — the replacement
 * PORT_SPEC.md's ground rule calls for ("hardcoded 1.12 recipes/machine recipes -> JSON
 * {@code Recipe<?>} types with serializers"), designed in full in
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md} Part B.
 * <p>
 * Package choice: {@code com.hbm.inventory.recipes} (this class + {@link HbmSimpleRecipe} living
 * directly in it), as the sibling of {@code com.hbm.inventory.recipes.loader} (CE's old Java-object
 * loader compatibility shim — see {@link com.hbm.inventory.recipes.loader.GenericRecipes}'s own
 * header for why that package is NOT where new recipe types belong) and of
 * {@code com.hbm.inventory.fluid}/{@code com.hbm.inventory.material}, this area's other
 * already-established {@code com.hbm.inventory.*} subpackages. Each future per-machine recipe
 * package (once that machine's block/TE exists) is expected to add its own
 * {@code RecipeType}/{@code RecipeSerializer} pair here (or in its own class following this exact
 * shape) rather than re-deriving the {@link DeferredRegister} boilerplate — that is this class's
 * whole purpose.
 * <p>
 * Follows this port's own already-confirmed-compiling {@code DeferredRegister.create(Registries.X,
 * MODID)} idiom (see {@code docs/phase0/material.md}, and this exact pattern already used for
 * {@code Registries.CREATIVE_MODE_TAB} in {@code ModCreativeTabs} and {@code Registries.MENU}
 * elsewhere) applied to the two standard vanilla recipe registries,
 * {@link Registries#RECIPE_TYPE}/{@link Registries#RECIPE_SERIALIZER} — both stable, unversioned
 * Mojang registry keys, not a NeoForge-specific extension, so this part of the shape is not at risk
 * even though the {@code Recipe<?>}/codec shapes it registers ({@link HbmSimpleRecipe}) are
 * unverified against a real build (see that class's own header).
 * <p>
 * {@code SIMPLE_TYPE}/{@code SIMPLE_SERIALIZER} below are a demonstration registration, not a real
 * machine recipe — no machine in this port consumes {@link HbmSimpleRecipe} yet. They exist so this
 * scaffolding is provably wired (registered from {@link com.hbm.main.MainRegistry}, present in the
 * mod's recipe registries) rather than dead unregistered classes, and so the next machine package
 * has a working end-to-end example to copy.
 */
public final class HbmRecipes {

    private HbmRecipes() {
    }

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MainRegistry.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MainRegistry.MODID);

    /** Demonstration {@link HbmSimpleRecipe} type — see class header. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<HbmSimpleRecipe>> SIMPLE_TYPE =
            RECIPE_TYPES.register("hbm_simple", () -> new RecipeType<HbmSimpleRecipe>() {
                @Override
                public String toString() {
                    return "hbm:hbm_simple";
                }
            });

    /** Demonstration {@link HbmSimpleRecipe} serializer — see class header. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HbmSimpleRecipe>> SIMPLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("hbm_simple", () -> HbmSimpleRecipe.Serializer.INSTANCE);

    public static void register(IEventBus modEventBus) {
        // Force static DeferredHolder fields onto RECIPE_SERIALIZERS before the bus bind.
        com.hbm.inventory.recipes.crafting.DynamicCraftingRecipes.bootstrap();
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
