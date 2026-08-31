package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
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

import java.util.ArrayList;
import java.util.List;

/**
 * JSON {@code Recipe<?>} shape for the assembler's multi-input recipes, following the same overall
 * pattern {@link HbmSimpleRecipe} demonstrates (a plain data record + {@code Serializer} nested
 * class, registered as its own {@link RecipeType}/{@link RecipeSerializer} pair) but generalized to
 * the shape CE's own assembler recipes actually need: up to a handful of {@link Ingredient}+count
 * input pairs (CE's {@code GenericRecipe.inputItem}, an {@code AStack[]}, capped at 12 slots per
 * {@code AssemblyMachineRecipes.inputItemLimit()}) and a per-recipe {@code duration}+{@code power}
 * (CE's assembler is the one machine of this file's family whose power draw is recipe-driven, not a
 * hardcoded TE constant - see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s
 * "Power draw" analysis: {@code maxPower = recipe.power * 100}).
 * <p>
 * Optional {@code input_fluids}/{@code output_fluids} match CE {@code GenericRecipe.inputFluids}/
 * {@code outputFluids} (AssemblyMachineRecipes.java:78-96 — sulfuric acid / lubricant / etc.).
 * {@link FluidStack} now has {@code Codec}/{@code StreamCodec}. Item-only recipes omit the fields.
 * <p>
 * <b>Recipe selection</b>: CE's real assembler picks one recipe by name via a blueprint item + GUI
 * dropdown ({@code TileEntityMachineAssemblyMachine.receiveControl}/{@code assemblerModule.recipe}),
 * reading {@code AssemblyMachineRecipes.INSTANCE.recipeNameMap}. That whole selection UI is
 * cross-cutting Phase-2 GUI work this task does not own (no dropdown/scroll-list widget exists in
 * this port's {@code GuiInfoContainer} yet). {@link com.hbm.blockentity.machine.MachineAssemblyMachineBlockEntity}
 * instead auto-detects whichever registered recipe the current 12 input slots satisfy (first match
 * wins, like a shapeless auto-crafter) - a documented simplification, not a silent behavior change:
 * every recipe in this family still requires the exact same items in the exact same quantities CE's
 * data specifies, just without the manual "select which blueprint" step. <b>TODO(items-followup)</b>:
 * once the blueprint-pool system lands, the block entity's blueprint slot can gate/narrow this
 * auto-detection instead of (or alongside) blind first-match.
 */
public class AssemblerRecipe implements Recipe<AssemblerRecipe.Input> {

    protected final String group;
    protected final List<Entry> inputs;
    protected final ItemStack output;
    protected final int duration;
    protected final long power;
    protected final List<FluidStack> inputFluids;
    protected final List<FluidStack> outputFluids;

    public AssemblerRecipe(String group, List<Entry> inputs, ItemStack output, int duration, long power) {
        this(group, inputs, output, duration, power, List.of(), List.of());
    }

    public AssemblerRecipe(String group, List<Entry> inputs, ItemStack output, int duration, long power,
                           List<FluidStack> inputFluids, List<FluidStack> outputFluids) {
        this.group = group;
        this.inputs = inputs;
        this.output = output;
        this.duration = duration;
        this.power = power;
        this.inputFluids = inputFluids == null ? List.of() : inputFluids;
        this.outputFluids = outputFluids == null ? List.of() : outputFluids;
    }

    public List<Entry> getInputEntries() {
        return inputs;
    }

    public int getDuration() {
        return duration;
    }

    public long getPower() {
        return power;
    }

    public List<FluidStack> getInputFluids() {
        return inputFluids;
    }

    public List<FluidStack> getOutputFluids() {
        return outputFluids;
    }

    /**
     * Bag-of-items multiset match: every configured {@link Entry} must be satisfiable by some
     * combination of the input's stacks, greedily consuming quantity as it goes (matching CE's own
     * {@code ModuleMachineBase.canProcess}, which likewise treats the 12 input slots as an unordered
     * pool rather than positional slots). Not itself called by
     * {@code MachineAssemblyMachineBlockEntity} (see class javadoc's "Recipe selection" note - the
     * block entity matches directly against its own inventory for a better error/partial-consumption
     * story), but kept correct so this class satisfies {@link Recipe}'s real contract for any other
     * caller (a future JEI/REI-style viewer, {@code RecipeManager} lookups, etc).
     */
    @Override
    public boolean matches(Input input, Level level) {
        int[] remaining = new int[input.size()];
        for (int i = 0; i < input.size(); i++) remaining[i] = input.getItem(i).getCount();

        for (Entry entry : inputs) {
            int needed = entry.count();
            for (int i = 0; i < input.size() && needed > 0; i++) {
                if (remaining[i] <= 0) continue;
                ItemStack stack = input.getItem(i);
                if (stack.isEmpty() || !entry.ingredient().test(stack)) continue;
                int take = Math.min(needed, remaining[i]);
                remaining[i] -= take;
                needed -= take;
            }
            if (needed > 0) return false;
        }
        return true;
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
        for (Entry entry : inputs) list.add(entry.ingredient());
        return list;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeType<?> getType() {
        return ProcessingRecipes.ASSEMBLER_TYPE.get();
    }

    @Override
    public RecipeSerializer<AssemblerRecipe> getSerializer() {
        return ProcessingRecipes.ASSEMBLER_SERIALIZER.get();
    }

    /** One (ingredient, count) input requirement - CE's {@code RecipesCommon.AStack} (item half only, see class javadoc). */
    public record Entry(Ingredient ingredient, int count) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Entry::ingredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Entry::count)
        ).apply(instance, Entry::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Entry::ingredient,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new
        );
    }

    /** Snapshot-based {@link RecipeInput} over an arbitrary number of unordered input slots. */
    public record Input(List<ItemStack> items) implements RecipeInput {

        public static Input of(List<ItemStack> stacks) {
            return new Input(List.copyOf(stacks));
        }

        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<AssemblerRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<AssemblerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Entry.CODEC.listOf().fieldOf("inputs").forGetter(r -> r.inputs),
                ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                Codec.INT.optionalFieldOf("duration", 0).forGetter(AssemblerRecipe::getDuration),
                Codec.LONG.optionalFieldOf("power", 0L).forGetter(AssemblerRecipe::getPower),
                FluidStack.CODEC.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(AssemblerRecipe::getInputFluids),
                FluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(AssemblerRecipe::getOutputFluids)
        ).apply(instance, AssemblerRecipe::new));

        // 7 fields — vanilla StreamCodec.composite tops out at 6.
        private static final StreamCodec<RegistryFriendlyByteBuf, List<Entry>> ENTRIES_STREAM =
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list());
        private static final StreamCodec<RegistryFriendlyByteBuf, List<FluidStack>> FLUIDS_STREAM =
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list());

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblerRecipe> STREAM_CODEC =
                StreamCodec.of((buf, r) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                    ENTRIES_STREAM.encode(buf, r.inputs);
                    ItemStack.STREAM_CODEC.encode(buf, r.output);
                    ByteBufCodecs.VAR_INT.encode(buf, r.duration);
                    ByteBufCodecs.VAR_LONG.encode(buf, r.power);
                    FLUIDS_STREAM.encode(buf, r.inputFluids);
                    FLUIDS_STREAM.encode(buf, r.outputFluids);
                }, buf -> new AssemblerRecipe(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ENTRIES_STREAM.decode(buf),
                        ItemStack.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf),
                        FLUIDS_STREAM.decode(buf),
                        FLUIDS_STREAM.decode(buf)
                ));

        @Override
        public MapCodec<AssemblerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    /** Convenience builder, mirroring CE's own {@code new GenericRecipe(name).setup(dur, pow).outputItems(...).inputItems(...)} chain shape. */
    public static AssemblerRecipe of(String group, ItemStack output, int duration, long power, Entry... entries) {
        return new AssemblerRecipe(group, new ArrayList<>(List.of(entries)), output, duration, power);
    }
}
