package com.hbm.inventory.recipes.crafting;

import com.hbm.items.weapon.grenade.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeItems;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.crafting.handlers.GrenadeCraftingHandler} (87 lines, read in full;
 * see {@code docs/phase7/crafting_dynamic_handlers.md} catalog entry 4) - combine exactly one shell +
 * one compatible filling + one fuze (+ an optional extra) anywhere in the grid into a thrown
 * grenade. All 26 component items and {@link ItemGrenadeUniversal#make} already exist
 * ({@code GrenadeItems.java}); this class is the "genuine new {@code Recipe<CraftingInput>} Java"
 * piece {@code GrenadeItems}'s own class javadoc flagged as the only missing piece.
 * <p>
 * <b>Item-family flattening carried over from matching</b>: CE matches by a single fixed
 * {@code Item} reference per category (one {@code ItemEnumMulti} each for shell/filling/fuze/extra,
 * varying by metadata) via its own {@code getFirst(inv, itemType, values)} helper, which returns
 * {@code null} (failing the match) the moment a <em>second</em> stack of that same {@code Item} type
 * turns up anywhere in the grid. This port instead registers 4/13/5/4 distinct items (one per enum
 * value, no shared {@code Item} to compare against) - {@link GrenadeItems#shellOf}/{@code fillingOf}/
 * {@code fuzeOf}/{@code extraOf} (added alongside this class) are the reverse lookup CE's metadata
 * read effectively performed, and this class fails the match the moment a <em>second</em> stack maps
 * to the same <em>category</em> (regardless of which specific enum value), reproducing CE's per-
 * category uniqueness check exactly despite the different underlying item model.
 * <p>
 * <b>Extra's uniqueness is not explicitly checked</b>, matching CE's own {@code matches()} (which
 * only calls {@code getFirst} for shell/filling/fuze, explicitly leaving extra "unaccounted for" per
 * its own comment) - CE's total-non-empty-stack cap of 4 (shell + filling + fuze + at most one more
 * slot) already makes a duplicate-extra scenario unreachable without also tripping that cap, so the
 * omission is provably a no-op rather than a real gap; this class still guards it defensively (see
 * {@link #scan}) since doing so costs nothing and only ever agrees with CE's real behavior.
 * <p>
 * <b>Unconfirmed API surface, flagged rather than silently assumed</b>: this class (and its 3
 * siblings in this package) implements {@code net.minecraft.world.item.crafting.CraftingRecipe}
 * (adding just {@link #category}) rather than plain {@code Recipe<CraftingInput>}, on the reasoning
 * that vanilla's client-side recipe-book grouping (which buckets every {@code RecipeType.CRAFTING}
 * recipe by category to build its tabs) most likely invokes a {@code CraftingRecipe}-typed method on
 * every such recipe eagerly (e.g. on login/recipe-sync), so a recipe that does not really implement
 * that interface risks an {@code IncompatibleClassChangeError} the first time any client opens or
 * syncs the recipe book - not just when this specific recipe is actually crafted. <b>This local
 * NeoForge source checkout does not itself contain any real usage of the bare name
 * {@code CraftingRecipe}</b> (grepped {@code /home/user/neoforged/neoforge} in full - zero hits,
 * unlike {@code CraftingInput}/{@code CraftingBookCategory}/{@code RecipeType.CRAFTING}, all of
 * which ARE independently confirmed real via this same checkout - see {@code RecipeBookTestRecipe}/
 * {@code ShapelessRecipe.java.patch}/{@code ResultSlot.java.patch} respectively), so this specific
 * class name/contract is carried over from general 1.20.2+ vanilla recipe-API knowledge, not
 * locally re-derived evidence, and should be the first thing double-checked against the real jar
 * before relying on it further - if it turns out not to exist (or to require more than
 * {@code category()}), the fix is a one-line interface swap back to {@code Recipe<CraftingInput>}
 * plus a plain (non-override) {@code category()} method, not a rewrite of any match/assemble logic.
 */
public final class GrenadeCraftingRecipe implements CraftingRecipe {

    public static final GrenadeCraftingRecipe INSTANCE = new GrenadeCraftingRecipe();

    private GrenadeCraftingRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Scanned scanned = scan(input);
        if (scanned == null) return false;
        if (scanned.shell == null || scanned.filling == null || scanned.fuze == null) return false;
        return scanned.filling.compatibleShells.contains(scanned.shell);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Scanned scanned = scan(input);
        if (scanned == null) return ItemStack.EMPTY;
        if (scanned.shell == null || scanned.filling == null || scanned.fuze == null) return ItemStack.EMPTY;
        if (!scanned.filling.compatibleShells.contains(scanned.shell)) return ItemStack.EMPTY;
        return ItemGrenadeUniversal.make(scanned.shell, scanned.filling, scanned.fuze, scanned.extra);
    }

    /**
     * Single pass over every grid slot classifying each non-empty stack into one of the 4 component
     * categories - CE's {@code hasForeignObject} (foreign item / >4 total) plus the 4
     * {@code getFirst} calls, fused into one loop. Returns {@code null} for "foreign object present"
     * or "more than 4 total non-empty stacks" (CE's {@code hasForeignObject}); otherwise returns
     * whichever of the 4 categories were found (any may be {@code null} if that category was absent
     * or duplicated - duplication also nulls that field, matching {@code getFirst}'s own "second
     * stack of this type -> null" rule).
     */
    private static Scanned scan(CraftingInput input) {
        EnumGrenadeShell shell = null;
        EnumGrenadeFilling filling = null;
        EnumGrenadeFuze fuze = null;
        EnumGrenadeExtra extra = null;
        boolean shellDup = false, fillingDup = false, fuzeDup = false, extraDup = false;
        int total = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            total++;
            if (total > 4) return null;

            Item item = stack.getItem();
            EnumGrenadeShell s = GrenadeItems.shellOf(item);
            if (s != null) {
                if (shell != null) shellDup = true;
                shell = s;
                continue;
            }
            EnumGrenadeFilling f = GrenadeItems.fillingOf(item);
            if (f != null) {
                if (filling != null) fillingDup = true;
                filling = f;
                continue;
            }
            EnumGrenadeFuze z = GrenadeItems.fuzeOf(item);
            if (z != null) {
                if (fuze != null) fuzeDup = true;
                fuze = z;
                continue;
            }
            EnumGrenadeExtra x = GrenadeItems.extraOf(item);
            if (x != null) {
                if (extra != null) extraDup = true;
                extra = x;
                continue;
            }
            return null; // foreign, non-grenade item present
        }

        return new Scanned(
                shellDup ? null : shell,
                fillingDup ? null : filling,
                fuzeDup ? null : fuze,
                extraDup ? null : extra);
    }

    private record Scanned(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, EnumGrenadeExtra extra) {
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    /** CE: {@code getRecipeOutput() = ItemStack.EMPTY} - fully dynamic, no static display shape. */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public String getGroup() {
        return "";
    }

    /** No fixed ingredient list to declare (dynamic match, like CE's own {@code getRecipeOutput() = EMPTY}). */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<GrenadeCraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    /**
     * No per-instance configurable data (fully hardcoded match/assemble logic, like every CE handler
     * in this family) - the codec always resolves to the one {@link #INSTANCE}, matching this port's
     * {@code TrueCondition}/{@code FalseCondition}-style {@code MapCodec.unit(INSTANCE)} singleton
     * pattern (net.neoforged.neoforge.common.conditions, confirmed real usage) rather than a
     * {@code RecordCodecBuilder} with zero real fields.
     */
    public static final class Serializer implements RecipeSerializer<GrenadeCraftingRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<GrenadeCraftingRecipe> CODEC = MapCodec.unit(GrenadeCraftingRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, GrenadeCraftingRecipe> STREAM_CODEC =
                StreamCodec.unit(GrenadeCraftingRecipe.INSTANCE);

        @Override
        public MapCodec<GrenadeCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GrenadeCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
