package com.hbm.items.bomb;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registers the flat, no-metadata "bomb component" items every nuke casing's {@code isReady()}/
 * {@code isFilled()} chain checks by identity, ported from CE's {@code com.hbm.items.bomb} package
 * (see {@code docs/phase3/bomb_blocks_and_detonators.md} Section B). CE backed each of these with its
 * own single-purpose item subclass ({@code ItemBoy}/{@code ItemGadget}/{@code ItemMan}/
 * {@code ItemManMike}/{@code ItemMike}/{@code ItemTsar}/{@code ItemFleija}/{@code ItemN2}/
 * {@code ItemSolinium}/{@code ItemCustomLore}) whose only real behavior was a decorative
 * "used in: &lt;tile&gt;" tooltip line (via {@code I18nUtil}) - none of that tooltip flavor text is
 * ported here (no gameplay effect, and this port's lang file doesn't carry those keys yet); every
 * item below is a plain {@link ItemBase}. This is a genuinely new item family this package's block
 * work depends on - the Phase 3 research report's assumption that these were already Phase 1/2
 * registry entries (per {@code docs/phase1/items_machine.md}) does not hold for CE's
 * {@code items/bomb} package specifically, confirmed absent by a repo-wide search before this class
 * was written.
 * <p>
 * Not registered here (confirmed real gaps, left as documented forward references rather than
 * fabricated under the wrong package): {@code ball_dynamite}/{@code ball_tnt}/{@code canister_napalm}
 * (CE {@code items/bomb}-adjacent but conceptually owned by the conventional-explosives/fluid-canister
 * families), and {@code ModBlocks.det_cord}/{@code det_charge}/{@code pink_barrel}/{@code yellow_barrel}
 * (blocks, not items, owned by the sibling conventional-explosives package) - all four are referenced
 * only by {@code TileEntityNukeCustom}'s crafting-entry map, and that map comments out the
 * corresponding {@code entries.put(...)} lines with a named TODO rather than registering content
 * outside this class's scope.
 */
public final class NukeCasingItems {

    // ==================== Gadget (implosion device) ====================
    public static DeferredItem<Item> EARLY_EXPLOSIVE_LENSES; // CE: early_explosive_lenses (gadget_explosive8) - shared with Man
    public static DeferredItem<Item> GADGET_WIREING;
    public static DeferredItem<Item> GADGET_CORE;

    // ==================== Little Boy (gun-type) ====================
    public static DeferredItem<Item> BOY_SHIELDING;
    public static DeferredItem<Item> BOY_TARGET;
    public static DeferredItem<Item> BOY_BULLET;
    public static DeferredItem<Item> BOY_PROPELLANT;
    public static DeferredItem<Item> BOY_IGNITER;

    // ==================== Fat Man / Ivy Mike / Tsar Bomba (staged) ====================
    public static DeferredItem<Item> MAN_CORE;
    public static DeferredItem<Item> MAN_IGNITER;
    public static DeferredItem<Item> EXPLOSIVE_LENSES; // CE: explosive_lenses (man_explosive8) - shared Mike/Tsar (NOT Man, which uses EARLY_EXPLOSIVE_LENSES)
    public static DeferredItem<Item> MIKE_CORE;
    public static DeferredItem<Item> MIKE_COOLING_UNIT;
    public static DeferredItem<Item> MIKE_DEUT;
    public static DeferredItem<Item> TSAR_CORE;

    // ==================== F.L.E.I.J.A. (schrabidium) ====================
    public static DeferredItem<Item> FLEIJA_IGNITER;
    public static DeferredItem<Item> FLEIJA_PROPELLANT;
    public static DeferredItem<Item> FLEIJA_CORE;

    // ==================== N2 mine ====================
    public static DeferredItem<Item> N2_CHARGE;

    // ==================== Balefire ====================
    public static DeferredItem<Item> EGG_BALEFIRE;
    public static DeferredItem<Item> EGG_BALEFIRE_SHARD;
    public static DeferredItem<Item> BATTERY_SPARK;
    public static DeferredItem<Item> BATTERY_TRIXITE;

    // ==================== Prototype (schrabidium test rig) ====================
    public static DeferredItem<Item> IGNITER; // CE: "igniter"/"trigger" - hand-held detonator for NukePrototype

    // ==================== NukeCustom crafting shortcuts + misc crafting materials it references ====================
    public static DeferredItem<Item> SOLINIUM_CORE;
    public static DeferredItem<Item> CUSTOM_TNT;
    public static DeferredItem<Item> CUSTOM_NUKE;
    public static DeferredItem<Item> CUSTOM_HYDRO;
    public static DeferredItem<Item> CUSTOM_AMAT;
    public static DeferredItem<Item> CUSTOM_DIRTY;
    public static DeferredItem<Item> CUSTOM_SCHRAB;
    public static DeferredItem<Item> CUSTOM_SOL;
    public static DeferredItem<Item> CUSTOM_FALL; // "falling bomb" mode marker for NukeCustom
    public static DeferredItem<Item> TRITIUM_DEUTERIUM_CAKE;
    public static DeferredItem<Item> NUCLEAR_WASTE;

    private NukeCasingItems() {
    }

    public static void registerAll() {
        EARLY_EXPLOSIVE_LENSES = reg1("early_explosive_lenses");
        GADGET_WIREING = reg1("gadget_wireing");
        GADGET_CORE = reg1("gadget_core");

        BOY_SHIELDING = reg1("boy_shielding");
        BOY_TARGET = reg1("boy_target");
        BOY_BULLET = reg1("boy_bullet");
        BOY_PROPELLANT = reg1("boy_propellant");
        BOY_IGNITER = reg1("boy_igniter");

        MAN_CORE = reg1("man_core");
        MAN_IGNITER = reg1("man_igniter");
        EXPLOSIVE_LENSES = reg1("explosive_lenses");
        MIKE_CORE = reg1("mike_core");
        MIKE_COOLING_UNIT = reg1("mike_cooling_unit");
        MIKE_DEUT = reg1("mike_deut");
        TSAR_CORE = reg1("tsar_core");

        FLEIJA_IGNITER = reg1("fleija_igniter");
        FLEIJA_PROPELLANT = reg1("fleija_propellant");
        FLEIJA_CORE = reg1("fleija_core");

        N2_CHARGE = reg("n2_charge", () -> new ItemBase(new Item.Properties().stacksTo(12)));

        EGG_BALEFIRE = reg1("egg_balefire");
        EGG_BALEFIRE_SHARD = reg("egg_balefire_shard", () -> new ItemBase(new Item.Properties().stacksTo(16)));
        BATTERY_SPARK = reg1("battery_spark");
        BATTERY_TRIXITE = reg1("battery_trixite");

        IGNITER = reg1("igniter");

        SOLINIUM_CORE = reg1("solinium_core");
        CUSTOM_TNT = reg1("custom_tnt");
        CUSTOM_NUKE = reg1("custom_nuke");
        CUSTOM_HYDRO = reg1("custom_hydro");
        CUSTOM_AMAT = reg1("custom_amat");
        CUSTOM_DIRTY = reg1("custom_dirty");
        CUSTOM_SCHRAB = reg1("custom_schrab");
        CUSTOM_SOL = reg1("custom_sol");
        CUSTOM_FALL = reg1("custom_fall");
        TRITIUM_DEUTERIUM_CAKE = reg1("tritium_deuterium_cake");
        NUCLEAR_WASTE = reg("nuclear_waste", () -> new ItemBase(new Item.Properties()));
    }

    /** Registers a stack-size-1 {@link ItemBase} in {@link ModCreativeTabs#NUKE} - the common case for every field above bar the few with an explicit stack size. */
    private static DeferredItem<Item> reg1(String name) {
        return reg(name, () -> new ItemBase(new Item.Properties().stacksTo(1)));
    }

    private static DeferredItem<Item> reg(String name, Supplier<? extends Item> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.NUKE, item);
        return item;
    }
}
