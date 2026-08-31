package com.hbm.inventory.recipes.crafting;

import com.hbm.blockentity.machine.CrateBlockEntity.CrateType;
import com.hbm.blocks.MaterialBlockGenerator;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.blocks.machine.StorageMachineBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.TagsUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Port of CE's {@code com.hbm.crafting.handlers.ContainerUpgradeCraftingHandler} (45 lines, read in
 * full; see {@code docs/phase7/crafting_dynamic_handlers.md} catalog entry 2) - fixed 3x3 shaped
 * upgrade recipes (crate/safe tiers) that additionally copy the source container's persistent
 * contents onto the output, so upgrading a full crate doesn't void its inventory.
 * <p>
 * <b>Only 3 of CE's 5 concrete instances are ported</b> ({@link Tier#CRATE_DESH}/
 * {@link Tier#CRATE_TUNGSTEN}/{@link Tier#SAFE}) - the 2 mass-storage tiers
 * ({@code mass_storage_desh}, {@code mass_storage}) are skipped: they need a {@code circuit}/
 * {@code circuit_*} item family and {@code mass_storage}/{@code mass_storage_iron}/
 * {@code mass_storage_desh} blocks/items, none of which exist anywhere in this port (confirmed by
 * the research report's item/registry dependency check and re-confirmed here by grep - zero hits).
 * See this task's {@code stillBlocked} report output for the exact missing ids.
 * <p>
 * <b>Shape composition, deliberately not {@code ShapedRecipePattern}</b>: the research report's
 * recommended shape composes vanilla's {@code ShapedRecipePattern} (the JSON-shape codec
 * {@code ShapedRecipe} itself uses) for {@code matches}/{@code getIngredients}, overriding only
 * {@code assemble()} for the data-component copy. This class hand-rolls the (small, fixed, always
 * exactly-3x3) positional match instead, using only the {@link CraftingInput#width()}/
 * {@link CraftingInput#height()}/{@link CraftingInput#getItem(int)} contract already confirmed real
 * by this exact NeoForge checkout's own
 * {@code net.neoforged.neoforge.oldtest.recipebook.RecipeBookTestRecipe} - {@code ShapedRecipePattern}
 * itself is real (confirmed via {@code ShapedRecipePattern.java.patch}) but its exact instance-method
 * names (e.g. whether {@code matches(CraftingInput)} takes one argument or two, and the precise
 * static-factory signature for building one outside of JSON) were not independently confirmed in
 * this sandbox, so this class trades a slightly larger hand-written matcher for zero dependency on
 * an unconfirmed API surface. All 5 of CE's real patterns happen to be left-right symmetric (see each
 * {@link Tier}'s pattern), so no mirror-matching is needed either way.
 * <p>
 * <b>Contents-preservation mechanism</b>: CE deep-copies the source container's whole NBT tag
 * compound onto the output. This port's equivalent "carries the block entity's saved contents on the
 * dropped item" pipeline is {@code com.hbm.blockentity.IPersistentNBT#breakBlock} (read in full),
 * which puts the persisted tag under {@link TagsUtil#putCustomData}/{@code CustomData} and separately
 * layers on {@link BlockStorageCrate#CRATE_RAD_KEY} (contained-item radiation) and
 * {@link DataComponents#CUSTOM_NAME} (custom name) as real data components - so {@link #copyContainerData}
 * copies all three explicitly rather than one raw NBT compound, matching CE's actual "preserve
 * everything the source item was carrying" intent through this port's split representation.
 */
public final class ContainerUpgradeRecipe implements CraftingRecipe {

    public enum Tier {
        /** CE {@code CraftingManager.java:1166}: ` D `/`DSD`/` D `, D=plate_desh, S=crate_steel -> crate_desh. */
        CRATE_DESH(
                new String[]{" D ", "DSD", " D "},
                map('D', ContainerUpgradeRecipe::plateDesh, 'S', ContainerUpgradeRecipe::crateSteel),
                'S',
                ContainerUpgradeRecipe::crateDesh),
        /** CE {@code CraftingManager.java:1171}: `BPB`/`PCP`/`BPB`, B=tungsten block, P=copper cast-plate, C=crate_steel -> crate_tungsten. */
        CRATE_TUNGSTEN(
                new String[]{"BPB", "PCP", "BPB"},
                map('B', ContainerUpgradeRecipe::tungstenBlock, 'P', ContainerUpgradeRecipe::copperCastPlate, 'C', ContainerUpgradeRecipe::crateSteel),
                'C',
                ContainerUpgradeRecipe::crateTungsten),
        /** CE {@code CraftingManager.java:1177}: `LAL`/`ACA`/`LAL`, L=lead plate, A=titanium plate, C=crate_steel -> safe. */
        SAFE(
                new String[]{"LAL", "ACA", "LAL"},
                map('L', ContainerUpgradeRecipe::plateLead, 'A', ContainerUpgradeRecipe::plateTitanium, 'C', ContainerUpgradeRecipe::crateSteel),
                'C',
                ContainerUpgradeRecipe::safe);

        final String[] pattern;
        final Map<Character, Supplier<Item>> key;
        final char containerKey;
        final Supplier<Item> output;

        Tier(String[] pattern, Map<Character, Supplier<Item>> key, char containerKey, Supplier<Item> output) {
            this.pattern = pattern;
            this.key = key;
            this.containerKey = containerKey;
            this.output = output;
        }
    }

    public static final ContainerUpgradeRecipe CRATE_DESH = new ContainerUpgradeRecipe(Tier.CRATE_DESH);
    public static final ContainerUpgradeRecipe CRATE_TUNGSTEN = new ContainerUpgradeRecipe(Tier.CRATE_TUNGSTEN);
    public static final ContainerUpgradeRecipe SAFE = new ContainerUpgradeRecipe(Tier.SAFE);

    private final Tier tier;

    /**
     * Private - construction never resolves any item/block (only stores the enum {@link Tier}), so
     * this class is safe to touch from a static field initializer (this class's own 3 singletons
     * above, and {@code DynamicCraftingRecipes}'s serializer registrations) before any
     * {@code RegisterEvent} has fired. Every method below that actually needs a real {@link Item}
     * (the {@link Tier#key}/{@link Tier#output} {@link Supplier}s) resolves it lazily, only when
     * {@code matches}/{@code assemble}/{@code getIngredients}/{@code getResultItem} are actually
     * invoked by the recipe manager - always well after registration completes. See this port's
     * established "no {@code DeferredHolder.get()} inside a static field initializer" rule
     * ({@code BlockModDoor.java}, commit 655f63c).
     */
    private ContainerUpgradeRecipe(Tier tier) {
        this.tier = tier;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char k = tier.pattern[row].charAt(col);
                ItemStack stack = input.getItem(col + row * 3);
                if (k == ' ') {
                    if (!stack.isEmpty()) return false;
                } else {
                    Supplier<Item> want = tier.key.get(k);
                    if (want == null || stack.isEmpty() || stack.getItem() != want.get()) return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (input.width() != 3 || input.height() != 3) return ItemStack.EMPTY;

        ItemStack containerSource = null;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char k = tier.pattern[row].charAt(col);
                ItemStack stack = input.getItem(col + row * 3);
                if (k == ' ') {
                    if (!stack.isEmpty()) return ItemStack.EMPTY;
                    continue;
                }
                Supplier<Item> want = tier.key.get(k);
                if (want == null || stack.isEmpty() || stack.getItem() != want.get()) return ItemStack.EMPTY;
                if (k == tier.containerKey) containerSource = stack;
            }
        }

        ItemStack result = new ItemStack(tier.output.get());
        if (containerSource != null) copyContainerData(containerSource, result);
        return result;
    }

    /**
     * CE: "if it {@code hasTagCompound()}, deep-copy that tag onto the shaped-recipe's normal
     * output" - see class javadoc for how this port's split representation (persistent NBT tag,
     * {@link BlockStorageCrate#CRATE_RAD_KEY}, {@link DataComponents#CUSTOM_NAME}) maps onto that
     * one CE tag copy.
     */
    private static void copyContainerData(ItemStack source, ItemStack result) {
        if (TagsUtil.hasCustomData(source)) {
            TagsUtil.putCustomData(result, TagsUtil.getCustomData(source).copy());
        }
        Double rad = source.get(BlockStorageCrate.CRATE_RAD_KEY.get());
        if (rad != null) {
            result.set(BlockStorageCrate.CRATE_RAD_KEY.get(), rad);
        }
        if (source.has(DataComponents.CUSTOM_NAME)) {
            result.set(DataComponents.CUSTOM_NAME, source.get(DataComponents.CUSTOM_NAME));
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(tier.output.get());
    }

    @Override
    public String getGroup() {
        return "";
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (String row : tier.pattern) {
            for (int col = 0; col < row.length(); col++) {
                char k = row.charAt(col);
                if (k == ' ') {
                    list.add(Ingredient.EMPTY);
                    continue;
                }
                Supplier<Item> want = tier.key.get(k);
                list.add(want == null ? Ingredient.EMPTY : Ingredient.of(want.get()));
            }
        }
        return list;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<ContainerUpgradeRecipe> getSerializer() {
        return switch (tier) {
            case CRATE_DESH -> DynamicCraftingRecipes.CONTAINER_UPGRADE_CRATE_DESH_SERIALIZER.get();
            case CRATE_TUNGSTEN -> DynamicCraftingRecipes.CONTAINER_UPGRADE_CRATE_TUNGSTEN_SERIALIZER.get();
            case SAFE -> DynamicCraftingRecipes.CONTAINER_UPGRADE_SAFE_SERIALIZER.get();
        };
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    // ==================== lazy item/block resolvers (never called at class-load time) ====================

    private static Item plateDesh() {
        return PlateCrystalWasteItems.PLATE_DESH.get();
    }

    private static Item plateLead() {
        return PlateCrystalWasteItems.PLATE_LEAD.get();
    }

    private static Item plateTitanium() {
        return PlateCrystalWasteItems.PLATE_TITANIUM.get();
    }

    private static Item crateSteel() {
        return StorageMachineBlocks.CRATES.get(CrateType.STEEL).get().asItem();
    }

    private static Item crateDesh() {
        return StorageMachineBlocks.CRATES.get(CrateType.DESH).get().asItem();
    }

    private static Item crateTungsten() {
        return StorageMachineBlocks.CRATES.get(CrateType.TUNGSTEN).get().asItem();
    }

    private static Item safe() {
        return StorageMachineBlocks.CRATES.get(CrateType.SAFE).get().asItem();
    }

    private static Item tungstenBlock() {
        var block = MaterialBlockGenerator.get(Mats.MAT_TUNGSTEN);
        if (block == null) {
            throw new IllegalStateException("ContainerUpgradeRecipe: no autogenerated block for MAT_TUNGSTEN");
        }
        return block.get().asItem();
    }

    /** CE: copper cast-plate = {@code MaterialShapes.CASTPLATE} of {@code MAT_COPPER} -> registry id {@code "copper_plate_triple"}. */
    private static Item copperCastPlate() {
        return resolveItem(MaterialShapes.CASTPLATE.buildRegistryName(Mats.MAT_COPPER));
    }

    /** Same resolve-by-id shape as {@code ModRecipeProvider#item(String)} ({@code getOptional} rather than the defaulted-to-AIR {@code get}/{@code getValue}), reused here since this class is not itself a subclass of that provider. */
    private static Item resolveItem(String path) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
        Item found = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (found == null || found == Items.AIR) {
            throw new IllegalStateException("ContainerUpgradeRecipe: item hbm:" + path + " is not registered");
        }
        return found;
    }

    private static Map<Character, Supplier<Item>> map(char k1, Supplier<Item> v1, char k2, Supplier<Item> v2) {
        Map<Character, Supplier<Item>> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<Character, Supplier<Item>> map(char k1, Supplier<Item> v1, char k2, Supplier<Item> v2, char k3, Supplier<Item> v3) {
        Map<Character, Supplier<Item>> m = map(k1, v1, k2, v2);
        m.put(k3, v3);
        return m;
    }

    /** No per-instance JSON-configurable data - each tier's fixed singleton is baked into its own registered serializer, one per tier. */
    public static final class Serializer implements RecipeSerializer<ContainerUpgradeRecipe> {

        private final MapCodec<ContainerUpgradeRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ContainerUpgradeRecipe> streamCodec;

        public Serializer(ContainerUpgradeRecipe fixed) {
            this.codec = MapCodec.unit(fixed);
            this.streamCodec = StreamCodec.unit(fixed);
        }

        @Override
        public MapCodec<ContainerUpgradeRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ContainerUpgradeRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
