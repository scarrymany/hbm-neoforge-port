package com.hbm.compat.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * Small shared helpers for {@code c11-jei-recipe-categories}'s ~13 {@code IRecipeCategory}
 * implementations - not itself a category, just plumbing every one of them needs:
 * <ul>
 *     <li>{@link #fluidIcon} - builds this port's own {@code ItemFluidIcon} "GUI helper item"
 *     stand-in for a fluid slot, exactly the technique CE's real plugin ({@code ItemFluidIcon.
 *     make(...)}) and neo-edition's real, compiling plugin ({@code FluidIconItem.make(...)},
 *     e.g. {@code RefineryRecipeHandler.java:77}) independently settled on for a fluid ingredient
 *     that isn't a real NeoForge {@code Fluid} (see {@code docs/phase5/jei_integration.md}'s
 *     headline finding #3 - this port's {@link com.hbm.inventory.fluid.FluidStack}/{@link FluidType}
 *     are a wholly bespoke non-vanilla system, same as CE's).</li>
 *     <li>{@link #fluidIconItem()} - lazily resolves the already-registered {@code hbm:fluid_icon}
 *     item by registry name, mirroring the exact lazy-registry-lookup pattern
 *     {@code CrystallizerRecipes#hbmBlock(String)} already established in this port (see that
 *     method's own javadoc for why a registry-name lookup, not a direct field reference, is the
 *     safe way to reach an item from outside its owning registration class) - safe here because
 *     every caller of this class only ever runs from inside a real JEI plugin's
 *     {@code registerCategories}/{@code registerRecipes}/a category's own {@code setRecipe}, all of
 *     which JEI only invokes long after this port's own {@code RegisterEvent}s have fired.</li>
 *     <li>{@link #vanillaRecipes} - the one piece of glue {@code docs/phase5/jei_integration.md}'s
 *     research pass did not fully resolve: this port's Shredder/Assembler/Breeder recipes are real
 *     JSON-datapack {@code net.minecraft.world.item.crafting.Recipe<?>} data loaded into
 *     {@link net.minecraft.world.item.crafting.RecipeManager} - a genuinely different timing
 *     source than every other category in this package (whose backing data is a plain static Java
 *     collection, populated eagerly at common-setup, same as neo-edition's own reference plugin
 *     deals with exclusively - see that report's "Known risks" list, which flagged this exact gap).
 *     Standard, ubiquitous JEI-plugin-authoring practice across the wider modding ecosystem for a
 *     custom JSON-backed {@code RecipeType<T>} is to read {@code Minecraft.getInstance().level.
 *     getRecipeManager().getAllRecipesFor(type)} from inside {@code registerRecipes} - this relies
 *     on JEI's own well-known behavior of deferring a plugin's actual data registration until the
 *     player has joined a world (so a real, synced {@code RecipeManager} already exists), not on
 *     anything specific to this port. <b>Unverified against a real, running client</b> in this
 *     sandbox (no jar, no launch, see this port's Phase 5 ground rules) - if a real build shows JEI
 *     calling {@code registerRecipes} before a world is joined after all, this method's
 *     {@code level == null} guard degrades to an empty list rather than crashing, and whoever next
 *     has real client access should switch this to JEI's proper live-refresh mechanism
 *     ({@code IRecipeManagerPlugin}, registered via {@code IRecipeCatalystRegistration}) if that
 *     is what a real run demonstrates is actually needed.</li>
 * </ul>
 */
public final class JeiUtil {

    private JeiUtil() {
    }

    private static Item fluidIconItem;

    /** @return this port's already-registered {@code hbm:fluid_icon} item ({@link com.hbm.items.machine.ItemFluidIcon}), resolved lazily by registry name. */
    public static Item fluidIconItem() {
        if (fluidIconItem == null) {
            fluidIconItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fluid_icon"));
        }
        return fluidIconItem;
    }

    /** @return a display-only {@code ItemFluidIcon} stack for the given {@link FluidStack}, or {@link ItemStack#EMPTY} for a null/empty/zero-amount input. */
    public static ItemStack fluidIcon(FluidStack stack) {
        if (stack == null || stack.type == null || stack.fill <= 0) return ItemStack.EMPTY;
        return ItemFluidIcon.make(fluidIconItem(), stack);
    }

    /** @return a display-only {@code ItemFluidIcon} stack for the given fluid type + amount, or {@link ItemStack#EMPTY} for a null type/non-positive amount. */
    public static ItemStack fluidIcon(FluidType type, int amount) {
        if (type == null || amount <= 0) return ItemStack.EMPTY;
        return ItemFluidIcon.make(fluidIconItem(), type, amount);
    }

    /** @return an {@code hbm:<registryName>} item, resolved lazily by registry name (same pattern as {@link #fluidIconItem()}). */
    public static Item hbmItem(String registryName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, registryName));
    }

    /** @return an {@code hbm:<registryName>} block's item form, resolved lazily by registry name - for a machine's JEI catalyst/category icon. */
    public static Item hbmBlockItem(String blockRegistryName) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, blockRegistryName)).asItem();
    }

    /**
     * @return the conventional {@code hbm:textures/gui/jei/gui_nei_<name>.png} path CE's own real
     * JEI plugin uses for several of these categories (confirmed for shredder/refinery, see
     * {@code docs/phase5/jei_integration.md}'s per-machine table) - pointed at deliberately even
     * though the file does not exist in this port's resources yet (see that report's headline
     * finding #4: 0 of CE's real 493 GUI textures are ported anywhere in this port today). NeoForge
     * renders its own missing-texture placeholder for an unresolvable {@link ResourceLocation}
     * rather than crashing - the same "point at the real eventual path, let the engine draw a
     * placeholder" convention {@code GuiInfoContainer#drawInfoPanel}'s own javadoc already
     * documents for this port's machine GUI screens. Once the sibling GUI-asset-porting task lands
     * the real PNG at this exact path, every category built against this helper picks it up with no
     * registration/slot-matching code changes.
     */
    public static ResourceLocation jeiTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/jei/gui_nei_" + name + ".png");
    }

    /** See this class's own javadoc "vanillaRecipes" bullet for the full timing-risk writeup. */
    public static <I extends RecipeInput, T extends Recipe<I>> List<T> vanillaRecipes(RecipeType<T> type) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return level.getRecipeManager().getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
    }
}
