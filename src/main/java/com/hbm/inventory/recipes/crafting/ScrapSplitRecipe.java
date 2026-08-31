package com.hbm.inventory.recipes.crafting;

import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.crafting.handlers.ScrapsCraftingHandler} (67 lines, read in full; see
 * {@code docs/phase7/crafting_dynamic_handlers.md} catalog entry 7) - split one scrap glob (amount
 * &gt;= 2) into two same-material scraps, each carrying half the amount (rounded down). A stack-
 * splitting utility, not a material-changing recipe - lets a large crucible-output glob be divided
 * into smaller usable castings.
 * <p>
 * <b>Item-family translation</b>: CE has one {@code ModItems.scraps} field (material = metadata,
 * amount = NBT); this port registers one {@code scraps_<material>} item per scrappable
 * {@link com.hbm.inventory.material.NTMMaterial} instead (amount lives in
 * {@link ItemScraps#getAmount}/a data component - see {@code ItemScraps}'s own class javadoc). The
 * match/assemble logic below keys off {@code instanceof ItemScraps} rather than a single fixed
 * {@code Item} reference; CE's implicit "same material" guarantee (its match loop fails the instant
 * a <em>second</em> non-empty stack of any kind appears, so there is never more than one material to
 * compare) carries over unchanged since {@link CraftingInputs#onlyNonEmptyStack} enforces the same
 * "exactly one stack" constraint.
 * <p>
 * <b>CE's liquid flag is dropped across the split, preserved not "fixed"</b>: CE's real
 * {@code getCraftingResult} calls {@code ItemScraps.create(Mats.MaterialStack)} - the 1-argument
 * overload, which defaults the additive/"liquid" flag to {@code false} regardless of whether the
 * source stack itself was liquid (CE's 2-argument {@code create(MaterialStack, boolean)} overload
 * exists but is never the one this handler calls). This class calls this port's equivalent
 * 3-argument {@link ItemScraps#create} with {@code liquid = false} for the same reason, matching
 * CE's actual behavior rather than "improving" it to carry the flag through.
 */
public final class ScrapSplitRecipe implements CraftingRecipe {

    public static final ScrapSplitRecipe INSTANCE = new ScrapSplitRecipe();

    private ScrapSplitRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack stack = CraftingInputs.onlyNonEmptyStack(input);
        if (stack == null) return false;
        if (!(stack.getItem() instanceof ItemScraps)) return false;
        return ItemScraps.getAmount(stack) >= 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack stack = CraftingInputs.onlyNonEmptyStack(input);
        if (stack == null || !(stack.getItem() instanceof ItemScraps)) return ItemStack.EMPTY;

        int amount = ItemScraps.getAmount(stack);
        if (amount < 2) return ItemStack.EMPTY;

        ItemStack half = ItemScraps.create(new ItemStack(stack.getItem()), amount / 2, false);
        half.setCount(2);
        return half;
    }

    /** CE: {@code canFit(width, height) = width * height >= 1} - any real grid. */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /**
     * CE: {@code getRecipeOutput() = new ItemStack(ModItems.scraps)} - a generic, material-less
     * display stack. This port has no single generic "scraps" item to reference (see class javadoc),
     * so the honest translation is {@link ItemStack#EMPTY} rather than picking one arbitrary
     * material's item to stand in for all of them.
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public String getGroup() {
        return "";
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
    public RecipeSerializer<ScrapSplitRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    /** No per-instance configurable data - see {@code GrenadeCraftingRecipe.Serializer}'s javadoc for the singleton-codec rationale. */
    public static final class Serializer implements RecipeSerializer<ScrapSplitRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<ScrapSplitRecipe> CODEC = MapCodec.unit(ScrapSplitRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, ScrapSplitRecipe> STREAM_CODEC =
                StreamCodec.unit(ScrapSplitRecipe.INSTANCE);

        @Override
        public MapCodec<ScrapSplitRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ScrapSplitRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
