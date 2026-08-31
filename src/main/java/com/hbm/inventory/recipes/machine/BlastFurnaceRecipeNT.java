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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * JSON {@code Recipe<?>} shape for CE's {@code com.hbm.inventory.recipes.BlastFurnaceRecipesNT}
 * (the newer Blast Furnace's steel/mingrade-copper/meteor-alloy table - see
 * {@code docs/phase7/mrec_13_arcfurnace_misc.md} section "BlastFurnaceRecipesNT"). CE's own
 * {@code BlastFurnaceRecipe} ({@code GenericRecipe} subclass, 23 ln, read in full) carries exactly
 * this shape: up to 2 item inputs (order-independent, {@code inputItemLimit()==2}), up to 2 item
 * outputs, a per-recipe {@code duration}, and zero fluid I/O ({@code inputFluidLimit()==0},
 * {@code outputFluidLimit()==0}).
 * <p>
 * <b>Naming - "NT" suffix kept deliberately</b>: CE itself has two unrelated, separately-versioned
 * "Blast Furnace" recipe classes - the older {@code @Deprecated BlastFurnaceRecipes} (this port's
 * {@code com.hbm.inventory.recipes.BlastFurnaceRecipes}, the Di-Furnace pair, a different task's
 * work, see that class's own javadoc) and this task's newer {@code BlastFurnaceRecipesNT} (backing
 * {@code TileEntityMachineBlastFurnace}). Both are legitimately named "BlastFurnaceRecipes" in CE, so
 * this class keeps CE's own "NT" disambiguator rather than colliding on the bare name (even though a
 * different Java package would technically compile fine) - matching this exact CE-side naming
 * collision, not inventing a new one.
 * <p>
 * Registered through the same {@code RecipeType}/{@code RecipeSerializer}
 * {@link com.hbm.inventory.recipes.HbmRecipes}-append convention {@link BreederRecipe}/
 * {@link BreederRecipes} already established (see those classes' own javadocs) - deliberately its
 * own class rather than reusing {@link com.hbm.inventory.recipes.HbmSimpleRecipe}: that shape is
 * strictly single-{@link Ingredient}-in/single-{@link ItemStack}-out, and CE's blast furnace recipe
 * shape genuinely needs a second, order-independent input slot (see {@link Input}/{@link #matches}).
 * <p>
 * <b>Second output field intentionally NOT carried yet</b>: 5 of CE's 15 real recipes
 * ({@code steelFromIngot/steelFromDust/steelFromOre/steelWithFlux/mingradeOre}) produce a second
 * {@code ingot_raw} (damage-value {@code Mats.MAT_SLAG}) byproduct alongside their primary steel/
 * copper output - {@code ingot_raw} is explicitly documented as skipped-by-design in this port's own
 * {@code IngotNuggetItems.java} (no per-material "raw slag" item exists under any id), so per this
 * task's own porting rule (only port entries whose ingredients AND outputs already exist), those 5
 * recipes - plus {@code meteorSword} (blocked on the never-ported {@code meteorite_sword_hardened}/
 * {@code _alloyed} pair) - are not registered by {@link BlastFurnaceRecipesNT}. The remaining 9 CE
 * recipes this class does carry are all single-output, so this class stays the simpler, fully-proven
 * single-output shape ({@link BreederRecipe}/{@link com.hbm.inventory.recipes.HbmSimpleRecipe}'s own
 * shape) rather than speculatively adding an unused, unverifiable second-output field/codec branch -
 * whoever eventually ports {@code ingot_raw} should extend this class with a documented
 * {@code Optional}-style secondary output at that point, not before.
 * <p>
 * <b>No consuming machine block/block-entity/GUI exists in this port yet</b> for the Blast Furnace
 * (confirmed by directory listing of {@code com.hbm.blocks.machine}/{@code com.hbm.blockentity} -
 * see the research report's "Machine block/block-entity existence check" section) - this class and
 * its JSON recipes are deliberately authored now as inert, correctly-shaped data waiting for that
 * block, the same precedent this port's own {@code achblastfurnace.json} advancement (which already
 * references the not-yet-existing {@code hbm:machine_blast_furnace} item id) already set. A future
 * Blast Furnace block entity should look up {@link BlastFurnaceRecipesNT#BLAST_FURNACE_TYPE} via
 * {@code Level#getRecipeManager()} the same way {@code MachineReactorBreedingBlockEntity} already
 * looks up {@link BreederRecipes#BREEDER_TYPE}.
 * <p>
 * <b>Unverified against a real NeoForge 1.21.1 build</b> (no network access in this sandbox - see
 * {@code HbmSimpleRecipe}'s own header for the identical caveat already accepted elsewhere in this
 * package): the {@link RecipeInput} custom {@link Input} record and the {@link Slot} codec pair are
 * written from the same well-established vanilla recipe-codec conventions {@link BreederRecipe}/
 * {@link com.hbm.inventory.recipes.HbmSimpleRecipe} already use, not confirmed by compiling.
 */
public class BlastFurnaceRecipeNT implements Recipe<BlastFurnaceRecipeNT.Input> {

    private final Slot inputA;
    private final Slot inputB;
    private final ItemStack output;
    private final int duration;

    public BlastFurnaceRecipeNT(Slot inputA, Slot inputB, ItemStack output, int duration) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.duration = duration;
    }

    public Slot getInputA() {
        return inputA;
    }

    public Slot getInputB() {
        return inputB;
    }

    /** Ticks to process - CE's per-recipe {@code duration} (400-1,200 across the 15 CE entries). */
    public int getDuration() {
        return duration;
    }

    /**
     * Order-independent 2-slot match, mirroring CE's own {@code BlastFurnaceRecipesNT.getRecipe
     * (ItemStack s0, ItemStack s1)} (tries both slot orderings against both recipe input slots).
     */
    @Override
    public boolean matches(Input input, Level level) {
        ItemStack first = input.first();
        ItemStack second = input.second();
        return (inputA.matches(first) && inputB.matches(second))
                || (inputA.matches(second) && inputB.matches(first));
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(inputA.ingredient());
        list.add(inputB.ingredient());
        return list;
    }

    @Override
    public RecipeType<?> getType() {
        return BlastFurnaceRecipesNT.BLAST_FURNACE_TYPE.get();
    }

    @Override
    public RecipeSerializer<BlastFurnaceRecipeNT> getSerializer() {
        return BlastFurnaceRecipesNT.BLAST_FURNACE_SERIALIZER.get();
    }

    /**
     * One (ingredient, minimum count) input slot - CE's {@code OreDictStack}/{@code ComparableStack}
     * both carry a stack size ({@code AL.dust()} x1 vs. {@code Items.CLAY_BALL} x7 in the same
     * recipe), but a plain vanilla {@link Ingredient} does not, so this small record pairs one back
     * on. {@code count} defaults to 1 (omit the JSON field entirely for a single-item input).
     */
    public record Slot(Ingredient ingredient, int count) {

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getCount() >= count && ingredient.test(stack);
        }

        static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Slot::ingredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Slot::count)
        ).apply(instance, Slot::new));

        static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Slot::ingredient,
                ByteBufCodecs.VAR_INT, Slot::count,
                Slot::new
        );
    }

    /**
     * Two-slot {@link RecipeInput} - vanilla ships {@code SingleRecipeInput} (1 slot, used by
     * {@link BreederRecipe}) and {@code CraftingInput} (variable-size grid), neither fits a fixed
     * 2-slot order-independent machine, so this is a small dedicated record following the same
     * shape {@code SingleRecipeInput} itself uses.
     */
    public record Input(ItemStack first, ItemStack second) implements RecipeInput {

        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> first;
                case 1 -> second;
                default -> throw new IndexOutOfBoundsException("No such Blast Furnace input slot: " + index);
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<BlastFurnaceRecipeNT> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<BlastFurnaceRecipeNT> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Slot.CODEC.fieldOf("input_a").forGetter(BlastFurnaceRecipeNT::getInputA),
                Slot.CODEC.fieldOf("input_b").forGetter(BlastFurnaceRecipeNT::getInputB),
                ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                Codec.INT.fieldOf("duration").forGetter(BlastFurnaceRecipeNT::getDuration)
        ).apply(instance, BlastFurnaceRecipeNT::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipeNT> STREAM_CODEC = StreamCodec.composite(
                Slot.STREAM_CODEC, BlastFurnaceRecipeNT::getInputA,
                Slot.STREAM_CODEC, BlastFurnaceRecipeNT::getInputB,
                ItemStack.STREAM_CODEC, r -> r.output,
                ByteBufCodecs.VAR_INT, BlastFurnaceRecipeNT::getDuration,
                BlastFurnaceRecipeNT::new
        );

        @Override
        public MapCodec<BlastFurnaceRecipeNT> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipeNT> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
