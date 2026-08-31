package com.hbm.items.weapon.grenade;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemDisperser;
import com.hbm.items.weapon.ItemGrenadeDynamite;
import com.hbm.items.weapon.ItemGrenadeFishing;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registered {@code Item}s for {@code com.hbm.items.weapon.grenade} plus the two legacy
 * {@code com.hbm.items.weapon} grenade items and the disperser/canister family - all of which live
 * outside the {@code ModBlocks}/{@code ModItems} shared files per this wave's race-avoidance
 * convention (see {@code ModItems.register}'s own per-package {@code registerAll()} call pattern,
 * e.g. {@code BilletPowderItems}).
 * <p>
 * <b>Metadata flattening.</b> Per {@code docs/phase3/grenades.md}'s "Key design/API decisions", CE's
 * 4 damage-value-keyed {@code ItemEnumMulti} component items ({@code ItemGrenadeShell}/
 * {@code Filling}/{@code Fuze}/{@code Extra}, 4/13/5/4 metadata variants) each become one distinct
 * registry entry per enum value below, matching this port's established metadata-flattening
 * convention for small material/variant families (e.g. {@code com.hbm.items.ItemAmmoEnums}). These
 * component items themselves carry no special item logic (CE's own {@code ItemEnumMulti} subclasses
 * for this family override nothing beyond the constructor) - they exist as {@link ItemGrenadeUniversal}'s
 * tooltip lookup targets via {@link #shellItem}/{@link #fillingItem}/{@link #fuzeItem}/
 * {@link #extraItem}, and are meant to be combined into a {@code grenade_universal} stack.
 * <p>
 * <b>Crafting-table recipe (Phase 7, {@code docs/phase7/crafting_dynamic_handlers.md}):</b> CE's
 * dynamic shell+filling+fuze+(extra) crafting-table recipe ({@code GrenadeCraftingHandler}, a
 * custom-match {@code IRecipe}, not a static shape) is now ported as
 * {@link com.hbm.inventory.recipes.crafting.GrenadeCraftingRecipe} - see that class's javadoc for
 * the 1.21.1 {@code Recipe<CraftingInput>} shape used. {@link #shellOf}/{@link #fillingOf}/
 * {@link #fuzeOf}/{@link #extraOf} below are that recipe's reverse item-to-enum lookup (this port
 * registers one distinct {@link Item} per enum value rather than CE's single metadata-varying
 * {@code Item}, so the recipe needs a way back from a matched stack's item to which enum value it
 * represents).
 */
public final class GrenadeItems {

    private static final Map<EnumGrenadeShell, DeferredItem<Item>> SHELLS = new EnumMap<>(EnumGrenadeShell.class);
    private static final Map<EnumGrenadeFilling, DeferredItem<Item>> FILLINGS = new EnumMap<>(EnumGrenadeFilling.class);
    private static final Map<EnumGrenadeFuze, DeferredItem<Item>> FUZES = new EnumMap<>(EnumGrenadeFuze.class);
    private static final Map<EnumGrenadeExtra, DeferredItem<Item>> EXTRAS = new EnumMap<>(EnumGrenadeExtra.class);

    // ==================== shell (4) ====================
    public static final DeferredItem<Item> GRENADE_SHELL_FRAG = registerShell(EnumGrenadeShell.FRAG);
    public static final DeferredItem<Item> GRENADE_SHELL_STICK = registerShell(EnumGrenadeShell.STICK);
    public static final DeferredItem<Item> GRENADE_SHELL_TECH = registerShell(EnumGrenadeShell.TECH);
    public static final DeferredItem<Item> GRENADE_SHELL_NUKE = registerShell(EnumGrenadeShell.NUKE);

    // ==================== filling (13) ====================
    public static final DeferredItem<Item> GRENADE_FILLING_POWDER = registerFilling(EnumGrenadeFilling.POWDER);
    public static final DeferredItem<Item> GRENADE_FILLING_HE = registerFilling(EnumGrenadeFilling.HE);
    public static final DeferredItem<Item> GRENADE_FILLING_DEMO = registerFilling(EnumGrenadeFilling.DEMO);
    public static final DeferredItem<Item> GRENADE_FILLING_INC = registerFilling(EnumGrenadeFilling.INC);
    public static final DeferredItem<Item> GRENADE_FILLING_WP = registerFilling(EnumGrenadeFilling.WP);
    public static final DeferredItem<Item> GRENADE_FILLING_CLUSTER = registerFilling(EnumGrenadeFilling.CLUSTER);
    public static final DeferredItem<Item> GRENADE_FILLING_EMP = registerFilling(EnumGrenadeFilling.EMP);
    public static final DeferredItem<Item> GRENADE_FILLING_PLASMA = registerFilling(EnumGrenadeFilling.PLASMA);
    public static final DeferredItem<Item> GRENADE_FILLING_LASER = registerFilling(EnumGrenadeFilling.LASER);
    public static final DeferredItem<Item> GRENADE_FILLING_CLUSTER_HEAVY = registerFilling(EnumGrenadeFilling.CLUSTER_HEAVY);
    public static final DeferredItem<Item> GRENADE_FILLING_NUCLEAR = registerFilling(EnumGrenadeFilling.NUCLEAR);
    public static final DeferredItem<Item> GRENADE_FILLING_NUCLEAR_DEMO = registerFilling(EnumGrenadeFilling.NUCLEAR_DEMO);
    public static final DeferredItem<Item> GRENADE_FILLING_SCHRAB = registerFilling(EnumGrenadeFilling.SCHRAB);

    // ==================== fuze (5) ====================
    public static final DeferredItem<Item> GRENADE_FUZE_S3 = registerFuze(EnumGrenadeFuze.S3);
    public static final DeferredItem<Item> GRENADE_FUZE_S7 = registerFuze(EnumGrenadeFuze.S7);
    public static final DeferredItem<Item> GRENADE_FUZE_S15 = registerFuze(EnumGrenadeFuze.S15);
    public static final DeferredItem<Item> GRENADE_FUZE_IMPACT = registerFuze(EnumGrenadeFuze.IMPACT);
    public static final DeferredItem<Item> GRENADE_FUZE_AIRBURST = registerFuze(EnumGrenadeFuze.AIRBURST);

    // ==================== extra (4) ====================
    public static final DeferredItem<Item> GRENADE_EXTRA_GLUE = registerExtra(EnumGrenadeExtra.GLUE);
    public static final DeferredItem<Item> GRENADE_EXTRA_PROXY_FUZE = registerExtra(EnumGrenadeExtra.PROXY_FUZE);
    public static final DeferredItem<Item> GRENADE_EXTRA_FRAG_SLEEVE = registerExtra(EnumGrenadeExtra.FRAG_SLEEVE);
    public static final DeferredItem<Item> GRENADE_EXTRA_TRIPLEX = registerExtra(EnumGrenadeExtra.TRIPLEX);

    // ==================== the crafted, thrown item ====================
    public static final DeferredItem<Item> GRENADE_UNIVERSAL = register("grenade_universal",
            () -> new ItemGrenadeUniversal(new Item.Properties()), ModCreativeTabs.WEAPON);

    // ==================== legacy single-purpose grenades ====================
    public static final DeferredItem<Item> STICK_DYNAMITE = register("stick_dynamite",
            () -> new ItemGrenadeDynamite(3, new Item.Properties()), ModCreativeTabs.WEAPON);
    public static final DeferredItem<Item> STICK_DYNAMITE_FISHING = register("stick_dynamite_fishing",
            () -> new ItemGrenadeFishing(3, new Item.Properties()), ModCreativeTabs.WEAPON);

    // ==================== disperser/canister family ====================
    public static final DeferredItem<Item> DISPERSER_CANISTER = register("disperser_canister",
            () -> new ItemDisperser(2000, new Item.Properties()), ModCreativeTabs.WEAPON);
    public static final DeferredItem<Item> GLYPHID_GLAND = register("glyphid_gland",
            () -> new ItemDisperser(4000, new Item.Properties()), ModCreativeTabs.WEAPON);
    /** CE: "ordinary ItemBases with no grenade logic of their own" (the empty-tank return item). */
    public static final DeferredItem<Item> DISPERSER_CANISTER_EMPTY = register("disperser_canister_empty",
            () -> new ItemBase(new Item.Properties()), ModCreativeTabs.WEAPON);
    public static final DeferredItem<Item> GLYPHID_GLAND_EMPTY = register("glyphid_gland_empty",
            () -> new ItemBase(new Item.Properties()), ModCreativeTabs.WEAPON);

    private GrenadeItems() {
    }

    /** No-op beyond forcing this class to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    public static Item shellItem(EnumGrenadeShell shell) {
        return SHELLS.get(shell).get();
    }

    public static Item fillingItem(EnumGrenadeFilling filling) {
        return FILLINGS.get(filling).get();
    }

    public static Item fuzeItem(EnumGrenadeFuze fuze) {
        return FUZES.get(fuze).get();
    }

    public static Item extraItem(EnumGrenadeExtra extra) {
        return EXTRAS.get(extra).get();
    }

    /**
     * Reverse lookup for {@link com.hbm.inventory.recipes.crafting.GrenadeCraftingRecipe} - which
     * {@link EnumGrenadeShell} (if any) {@code item} is the registered item for. Small linear scan
     * (4 entries max) rather than a cached reverse map, so it never touches {@link DeferredItem#get()}
     * until actually called (i.e. never at class-load time - see this port's established
     * "no {@code DeferredHolder.get()} inside a static field initializer" rule).
     */
    @Nullable
    public static EnumGrenadeShell shellOf(Item item) {
        for (Map.Entry<EnumGrenadeShell, DeferredItem<Item>> entry : SHELLS.entrySet()) {
            if (entry.getValue().get() == item) return entry.getKey();
        }
        return null;
    }

    /** @see #shellOf(Item) */
    @Nullable
    public static EnumGrenadeFilling fillingOf(Item item) {
        for (Map.Entry<EnumGrenadeFilling, DeferredItem<Item>> entry : FILLINGS.entrySet()) {
            if (entry.getValue().get() == item) return entry.getKey();
        }
        return null;
    }

    /** @see #shellOf(Item) */
    @Nullable
    public static EnumGrenadeFuze fuzeOf(Item item) {
        for (Map.Entry<EnumGrenadeFuze, DeferredItem<Item>> entry : FUZES.entrySet()) {
            if (entry.getValue().get() == item) return entry.getKey();
        }
        return null;
    }

    /** @see #shellOf(Item) */
    @Nullable
    public static EnumGrenadeExtra extraOf(Item item) {
        for (Map.Entry<EnumGrenadeExtra, DeferredItem<Item>> entry : EXTRAS.entrySet()) {
            if (entry.getValue().get() == item) return entry.getKey();
        }
        return null;
    }

    /**
     * {@link EnumGrenadeShell#getStackLimit()} is the *crafted* {@code grenade_universal} stack's
     * limit (see {@link ItemGrenadeUniversal#getMaxStackSize}) - not this raw crafting-component
     * card's own stack size, which CE's {@code ItemGrenadeShell} (an {@code ItemEnumMulti}) never
     * overrides either. Plain default stack size here is correct, not an oversight.
     */
    private static DeferredItem<Item> registerShell(EnumGrenadeShell shell) {
        DeferredItem<Item> item = register("grenade_shell_" + shell.getSerializedName(), () -> new Item(new Item.Properties()), ModCreativeTabs.WEAPON);
        SHELLS.put(shell, item);
        return item;
    }

    private static DeferredItem<Item> registerFilling(EnumGrenadeFilling filling) {
        DeferredItem<Item> item = register("grenade_filling_" + filling.getSerializedName(), () -> new Item(new Item.Properties()), ModCreativeTabs.WEAPON);
        FILLINGS.put(filling, item);
        return item;
    }

    private static DeferredItem<Item> registerFuze(EnumGrenadeFuze fuze) {
        DeferredItem<Item> item = register("grenade_fuze_" + fuze.getSerializedName(), () -> new Item(new Item.Properties()), ModCreativeTabs.WEAPON);
        FUZES.put(fuze, item);
        return item;
    }

    private static DeferredItem<Item> registerExtra(EnumGrenadeExtra extra) {
        DeferredItem<Item> item = register("grenade_extra_" + extra.getSerializedName(), () -> new Item(new Item.Properties()), ModCreativeTabs.WEAPON);
        EXTRAS.put(extra, item);
        return item;
    }

    private static DeferredItem<Item> register(String name, Supplier<Item> supplier, ResourceKey<CreativeModeTab> tab) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, supplier);
        CreativeTabContents.add(tab, item);
        return item;
    }
}
