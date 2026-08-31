package com.hbm.inventory;

import com.hbm.capability.NTMFluidCapabilityHandler;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * CE's registry mapping fluid-filled item variants (buckets/canisters/gas tanks/drums) to their
 * fluid type and capacity, used to drive the generic "any item that looks like a fluid container
 * gets a fluid-handler capability" mechanism. Ported from CE's
 * {@code com.hbm.inventory.FluidContainerRegistry} (265 lines, read in full), which this port's
 * {@link NTMFluidCapabilityHandler} and {@link com.hbm.capability.NTMFluidContainerWrapper} already
 * depend on (both real, already-committed files - this class fills the compile gap they were left
 * with). Every query method below ({@link #getFluidContainer}, {@link #getFillRecipe(ItemStack, FluidType)},
 * {@link #getFillRecipe(ItemStack, Fluid)}, {@link #getFillRecipes}, {@link #getMaxFillCapacity},
 * {@link #getFluidContent(ItemStack)}, {@link #getFluidContent(ItemStack, FluidType)},
 * {@link #getFluidType(ItemStack)}, {@link #getFullContainer}, {@link #getEmptyContainer}, the
 * {@link #allContainers} field, and the {@link FluidContainer} record shape) is CE's own signature
 * verified directly against those two consuming files' real call sites - no adaptation of the public
 * API was needed beyond the two structural changes below.
 *
 * <h2>Two deliberate structural changes from CE</h2>
 * <ol>
 *   <li><b>No metadata dimension.</b> CE keyed both lookup tables on {@code (Item, metadata)} pairs
 *   ({@code Int2ObjectOpenHashMap<FluidContainer>} nested under a per-item map), because 1.12.2 used
 *   one item with many damage-value variants (one canister item, one metadata per fluid). 1.21 has no
 *   metadata - separate items replace metadata variants entirely, exactly the same reasoning
 *   {@link NTMFluidCapabilityHandler}'s own javadoc already documents for
 *   {@code isLeadSafeForgeContainer}'s whitelist. So the maps here are keyed on plain {@link Item}
 *   (one item maps to at most one "full" {@link FluidContainer}), dropping the metadata indirection
 *   and CE's {@code OreDictionary.WILDCARD_VALUE} meta-fallback lookup entirely.</li>
 *   <li><b>No ore-dictionary tag.</b> CE's {@code registerContainer} called
 *   {@code OreDictionary.registerOre(con.type().getDict(con.content()), con.fullContainer())} so
 *   other mods could look up "a fluid container of type X" via a runtime-registered ore-dictionary
 *   tag. Forge's {@code OreDictionary} does not exist in NeoForge - the modern equivalent (item tags)
 *   is data-driven from JSON at datapack-reload time, not a runtime Java registration call, so there
 *   is no drop-in replacement call to make here. Dropped, not silently: cross-mod fluid-container
 *   tag interop is a data-generator follow-up, not a runtime-registry concern.</li>
 * </ol>
 *
 * <h2>{@link #register()}'s content, vs. CE's ~40 {@code registerContainer} calls</h2>
 * <p>CE's {@code register()} populates the table almost entirely from item families that do not
 * exist in this port under a compatible shape (confirmed by searching the whole {@code src/main/java}
 * tree, not assumed): CE's per-fluid metadata items ({@code canister_full}, {@code gas_full},
 * {@code fluid_tank_full}/{@code fluid_tank_lead_full}/{@code fluid_barrel_full}, the
 * {@code disperser_canister}/{@code glyphid_gland} dynamic-fluid loop, {@code cell}) were all
 * superseded in this port by a <em>single fixed-capacity item carrying its current fluid as a data
 * component</em> ({@link com.hbm.items.machine.ItemFluidTank}/{@code V2},
 * {@link com.hbm.items.tool.ItemCanister}, {@link com.hbm.items.special.ItemCell}, and
 * {@link com.hbm.items.weapon.ItemDisperser}, all already committed) - a design that is fundamentally
 * incompatible with this registry's "one specific item (+meta) = one specific fluid" model, since one
 * of those items can hold <em>any</em> of dozens of fluid types simultaneously depending on its own
 * component state. Those items' own javadocs already document that their fill/drain capability wiring
 * is a follow-up of a different shape than this registry (a component-reading wrapper, not this
 * item-swapping one) - registering them here would be actively wrong, not just incomplete, so they are
 * intentionally left out rather than mis-registered. CE's remaining static entries
 * ({@code bottle_mercury}/{@code ingot_mercury}, the red/pink/lox-barrel-to-{@code tank_steel} set,
 * {@code rod_zirnox_tritium}, {@code particle_hydrogen}/{@code particle_amat}/{@code particle_aschrab},
 * {@code iv_blood}/{@code iv_xp}) reference items that do not exist anywhere in this port yet in any
 * form (confirmed by search) - CE's exact calls are preserved below as commented-out reference code so
 * a future item-family port can reactivate them verbatim once those items land, matching the
 * "commented-out, not deleted" precedent CE's own {@code register()} already sets for its vanilla
 * bucket/bottle block (superseded by the capability wrapper, per CE's own {@code mlbv} comment).
 * <p>
 * Four of CE's entries <em>are</em> faithfully portable today, because their underlying items are
 * real, already-registered, and keep the classic "one plain item = one fixed fluid" shape CE assumed:
 * the {@code ore_oil}/{@code ore_gneiss_gas} raw-ore blocks (real {@link com.hbm.blocks.OreBlocks}
 * entries, empty container {@code null} exactly like CE), {@code can_mug}/{@code can_empty} (real
 * {@link com.hbm.items.food.FoodItems} entries - {@code can_mug} always holds a fixed
 * {@link Fluids#MUG}, unlike the per-fluid canister/gas items above), and the vanilla
 * {@code Items.EXPERIENCE_BOTTLE}/{@code Items.GLASS_BOTTLE} pair. These four are registered as live
 * code below, resolved by registry name via {@link BuiltInRegistries} (the same lazy-resolve-by-name
 * idiom {@code OilDrillBaseBlockEntity#resolve} already established in this port) rather than a direct
 * field reference, since none of the three owning classes expose a public field for these items.
 */
public class FluidContainerRegistry {

    public static final Set<FluidContainer> allContainers = new ObjectOpenHashSet<>();
    private static final Reference2ObjectMap<Item, FluidContainer> fullContainerMapByItem = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectMap<Item, Candidates> emptyContainerMapByItem = new Reference2ObjectOpenHashMap<>();

    private static boolean registered = false;

    private FluidContainerRegistry() {}

    /**
     * Populates the registry, then calls {@link NTMFluidCapabilityHandler#initialize()} - matching
     * CE's own {@code register()}, whose last line is that exact call (CE line 107). Idempotent (this
     * port calls it from {@link com.hbm.capability.ModCapabilities#register}, a
     * {@code RegisterCapabilitiesEvent} listener that runs once in practice but is defensively
     * re-entrant-safe here, the same guard style {@link NTMFluidCapabilityHandler#initialize()} itself
     * already uses) and safe to call any time after every item/block {@code RegisterEvent} has fired.
     */
    public static void register() {
        if (registered) return;
        registered = true;

        // ---- Faithfully portable (see class javadoc for why only these four) ----

        registerContainer(new FluidContainer(new ItemStack(oreOil()), null, Fluids.OIL, 250));
        registerContainer(new FluidContainer(new ItemStack(oreGneissGas()), null, Fluids.PETROLEUM,
                GeneralConfig.enable528() ? 50 : 250));

        registerContainer(new FluidContainer(new ItemStack(resolveItem("can_mug")), new ItemStack(resolveItem("can_empty")),
                Fluids.MUG, 100));
        registerContainer(new FluidContainer(new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(Items.GLASS_BOTTLE),
                Fluids.XPJUICE, 100));

        // ---- Deferred: CE calls preserved verbatim for a future item-family port to reactivate ----
        // (see class javadoc, "CE's remaining static entries", for why each is commented out)

        // Vanilla buckets/bottles - CE has these commented out too (superseded by the capability
        // wrapper, CE's own "mlbv" note at the top of register()):
        // registerContainer(new FluidContainer(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BUCKET), Fluids.WATER, 1000));
        // registerContainer(new FluidContainer(new ItemStack(Items.POTIONITEM), new ItemStack(Items.GLASS_BOTTLE), Fluids.WATER, 250));
        // registerContainer(new FluidContainer(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.BUCKET), Fluids.LAVA, 1000));

        // Barrels -> tanks: needs ModBlocks.red_barrel/pink_barrel/lox_barrel and ModItems.tank_steel,
        // none of which exist in this port yet.
        // registerContainer(new FluidContainer(new ItemStack(ModBlocks.red_barrel), new ItemStack(ModItems.tank_steel), Fluids.DIESEL, 10000));
        // registerContainer(new FluidContainer(new ItemStack(ModBlocks.pink_barrel), new ItemStack(ModItems.tank_steel), Fluids.KEROSENE, 10000));
        // registerContainer(new FluidContainer(new ItemStack(ModBlocks.lox_barrel), new ItemStack(ModItems.tank_steel), Fluids.OXYGEN, 10000));

        // Mercury bottle/ingot: needs ModItems.bottle_mercury/ingot_mercury, neither of which exist in
        // this port yet (only the unrelated nugget_mercury/nugget_mercury_tiny drop items do).
        // registerContainer(new FluidContainer(new ItemStack(ModItems.bottle_mercury), new ItemStack(Items.GLASS_BOTTLE), Fluids.MERCURY, 1000));
        // registerContainer(new FluidContainer(new ItemStack(ModItems.ingot_mercury), null, Fluids.MERCURY, 125));

        // Zirnox tritium fuel rod: needs ModItems.rod_zirnox_tritium/rod_zirnox_empty. This port's
        // MachineItems only has rod_zirnox_<type>/rod_zirnox_depleted_<type> (a different family, no
        // fillable-tritium-load variant).
        // registerContainer(new FluidContainer(new ItemStack(ModItems.rod_zirnox_tritium), new ItemStack(ModItems.rod_zirnox_empty), Fluids.TRITIUM, 2000));

        // Particles: needs ModItems.particle_hydrogen/particle_amat/particle_aschrab/particle_empty,
        // none of which exist in this port yet (particle_muon/particle_digamma are unrelated items).
        // registerContainer(new FluidContainer(new ItemStack(ModItems.particle_hydrogen), new ItemStack(ModItems.particle_empty), Fluids.HYDROGEN, 1000));
        // registerContainer(new FluidContainer(new ItemStack(ModItems.particle_amat), new ItemStack(ModItems.particle_empty), Fluids.AMAT, 1000));
        // registerContainer(new FluidContainer(new ItemStack(ModItems.particle_aschrab), new ItemStack(ModItems.particle_empty), Fluids.ASCHRAB, 1000));

        // IVs: needs ModItems.iv_blood/iv_empty/iv_xp/iv_xp_empty, none of which exist in this port yet.
        // registerContainer(new FluidContainer(new ItemStack(ModItems.iv_blood), new ItemStack(ModItems.iv_empty), Fluids.BLOOD, 100));
        // registerContainer(new FluidContainer(new ItemStack(ModItems.iv_xp), new ItemStack(ModItems.iv_xp_empty), Fluids.XPJUICE, 100));

        // Dynamic per-fluid containers (canister/gas tank/disperser/glyphid gland/lead+plain fluid
        // tank/fluid barrel) and the SpecialContainerFillLists.EnumCell-driven cell loop: architecturally
        // incompatible with this port's component-based single-item design for every one of those item
        // families (see class javadoc, item 2 of "CE's remaining static entries" is wrong here - this
        // is the "actively wrong to register" case, not a missing-item case). Not reproduced in any form.

        NTMFluidCapabilityHandler.initialize();

        MainRegistry.logger.info("FluidContainerRegistry: registered {} fluid container(s).", allContainers.size());
    }

    public static void registerContainer(FluidContainer con) {
        allContainers.add(con);

        final Item fullItem = con.fullContainer().getItem();
        fullContainerMapByItem.put(fullItem, con);

        ItemStack empty = con.emptyContainer();
        if (empty != null && !empty.isEmpty()) {
            final Item emptyItem = empty.getItem();
            Candidates bucket = emptyContainerMapByItem.computeIfAbsent(emptyItem, k -> new Candidates());
            bucket.byType.put(con.type(), con);
            bucket.asList.add(con);
            if (con.content() > bucket.maxCapacity) bucket.maxCapacity = con.content();
        }
    }

    /**
     * @return amount of a specific fluid in the given full container stack.
     */
    @Contract(pure = true)
    public static int getFluidContent(ItemStack stack, FluidType type) {
        if (stack == null || stack.isEmpty() || type == null) return 0;
        FluidContainer recipe = getFluidContainer(stack);
        return (recipe != null && recipe.type() == type) ? recipe.content() : 0;
    }

    @Contract(pure = true)
    public static int getFluidContent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        FluidContainer recipe = getFluidContainer(stack);
        return recipe != null ? recipe.content() : 0;
    }

    /**
     * Gets the FluidType contained in a full container stack.
     */
    @NotNull
    @Contract(pure = true, value = "_->!null")
    public static FluidType getFluidType(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Fluids.NONE;
        FluidContainer recipe = getFluidContainer(stack);
        return recipe != null ? recipe.type() : Fluids.NONE;
    }

    /**
     * Gets the full container item for a given empty container and fluid type. Count insensitive.
     * @return a copy of the full container item for the given empty container and fluid type, or null if none is found.
     */
    @Nullable
    @Contract(pure = true, value = "null,_ -> null; _,null -> null")
    public static ItemStack getFullContainer(ItemStack stack, FluidType type) {
        if (stack == null || stack.isEmpty() || type == null) return null;
        FluidContainer recipe = getFillRecipe(stack, type);
        return recipe != null ? recipe.fullContainer().copy() : null;
    }

    /**
     * Gets the empty container item for a given full container stack.
     * @return a copy of the empty container item for the given full container stack, or null if none is found.
     */
    @Contract(pure = true)
    public static ItemStack getEmptyContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        FluidContainer recipe = getFluidContainer(stack);
        if (recipe != null && recipe.emptyContainer() != null) return recipe.emptyContainer().copy();
        return ItemStack.EMPTY;
    }

    /**
     * @return the FluidContainer of the given full container stack, or null if none is found.
     */
    @Nullable
    @Contract(pure = true)
    public static FluidContainer getFluidContainer(@NotNull ItemStack fullStack) {
        if (fullStack.isEmpty()) return null;
        return fullContainerMapByItem.get(fullStack.getItem());
    }

    /**
     * @return the FluidContainer of the given empty container and FluidType, or null if none is found.
     */
    @Nullable
    @Contract(pure = true, value = "_,null -> null")
    public static FluidContainer getFillRecipe(@NotNull ItemStack emptyStack, @Nullable FluidType type) {
        if (emptyStack.isEmpty() || type == null) return null;
        Candidates bucket = emptyContainerMapByItem.get(emptyStack.getItem());
        return bucket == null ? null : bucket.byType.get(type);
    }

    @Nullable
    @Contract(pure = true)
    public static FluidContainer getFillRecipe(@NotNull ItemStack emptyStack, @NotNull Fluid fluid) {
        return getFillRecipe(emptyStack, NTMFluidCapabilityHandler.getFluidType(fluid));
    }

    /**
     * Gets all possible fill recipes for a given empty item stack.
     *
     * @return A list of possible FluidContainer recipes, or an empty list if none are found.
     * @apiNote the returned List must not be modified.
     */
    @NotNull
    @Contract(pure = true, value = "_->!null")
    public static List<FluidContainer> getFillRecipes(@NotNull ItemStack emptyStack) {
        if (emptyStack.isEmpty()) return Collections.emptyList();
        Candidates bucket = emptyContainerMapByItem.get(emptyStack.getItem());
        return bucket != null ? bucket.asList : Collections.emptyList();
    }

    @Contract(pure = true)
    public static int getMaxFillCapacity(@NotNull ItemStack emptyStack) {
        if (emptyStack.isEmpty()) return 0;
        Candidates bucket = emptyContainerMapByItem.get(emptyStack.getItem());
        return bucket == null ? 0 : bucket.maxCapacity;
    }

    /** Lazily resolves a {@code hbm:}-namespaced item by registry path (same idiom as {@code OilDrillBaseBlockEntity#resolve}). */
    private static Item resolveItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /** Lazily resolves a {@code hbm:}-namespaced block by registry path (same idiom as {@code OilDrillBaseBlockEntity#resolve}). */
    private static Block resolveBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static Block oreOil() {
        return resolveBlock("ore_oil");
    }

    private static Block oreGneissGas() {
        return resolveBlock("ore_gneiss_gas");
    }

    public record FluidContainer(@NotNull ItemStack fullContainer, @Nullable ItemStack emptyContainer,
                                 @NotNull FluidType type, int content) {
    }

    private static final class Candidates {
        final Reference2ObjectOpenHashMap<FluidType, FluidContainer> byType = new Reference2ObjectOpenHashMap<>(4);
        final ObjectArrayList<FluidContainer> asList = new ObjectArrayList<>(4);
        int maxCapacity;
    }
}
