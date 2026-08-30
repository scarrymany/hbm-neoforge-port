package com.hbm.items.weapon.sedna.mods;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunDataComponents;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.XFactory22lr;
import com.hbm.items.weapon.sedna.content.XFactory357;
import com.hbm.items.weapon.sedna.content.XFactory44;
import com.hbm.items.weapon.sedna.content.XFactory50;
import com.hbm.items.weapon.sedna.content.XFactory556mm;
import com.hbm.items.weapon.sedna.content.XFactory762mm;
import com.hbm.items.weapon.sedna.content.XFactory9mm;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mods.XWeaponModManager} (417 lines) - the mod
 * eval dispatcher every {@code GunConfig}/{@code Receiver} getter routes through (Package C, per
 * {@code docs/phase3/gun_framework.md}'s work-split, read in full first). "The mod manager operates
 * by scraping upgrades from a gun, then iterating over them and evaluating the given value, passing
 * the modified value to successive mods. The way that mods stack (additive vs multiplicative) depends
 * on the order the mod is installed in" - CE's own class javadoc, still exactly true here.
 * <p>
 * <b>Id scheme - a deliberate deviation from CE, not an oversight.</b> CE assigns each
 * {@code IWeaponMod} a raw {@code int} via magic-number constructor arguments
 * ({@code new WeaponModSilencer(201)}), tracked forward/backward in a
 * {@code HashBiMap<Integer, IWeaponMod>} and persisted as a per-config-index {@code int[]} NBT array
 * ({@code KEY_MOD_LIST_N}) - the exact same "fragile construction-order id" anti-pattern
 * {@code BulletConfig}'s own javadoc already documents fixing for ammo. This port applies the
 * identical fix here: every {@link IWeaponMod} is constructed with an explicit, human-readable
 * {@link ResourceLocation} id (see {@link WeaponModBase}), the registry is a plain
 * {@code Map<ResourceLocation, IWeaponMod>}, and the installed-mod list persists as
 * {@link WeaponModDataComponents#MOD_LISTS} ({@code List<List<String>>}, id strings, not ints) -
 * stable across reorderings, reloads, and save round-trips, unlike CE's transient network/session ids.
 * <p>
 * <b>{@code weapon_mod_test} (CE's 10-variant debug item family: {@code FIRERATE}/{@code DAMAGE}/
 * {@code MULTI}/7x {@code OVERRIDE_*}) is deliberately not ported</b>, matching this task's explicit
 * instruction to skip the 3 {@code WeaponModTest*} debug mod classes those items exclusively install -
 * see {@link WeaponModItems}'s class javadoc for the item-family side of the same exclusion.
 * <p>
 * <b>Forward-reference gun resolution, not a hardcoded compile-time dependency.</b> CE's own
 * {@code init()} references every gun by a direct {@code ModItems.gun_xxx} static field, which only
 * compiles because CE's {@code GunFactory} + every {@code XFactory*} content file all live in one
 * always-fully-built source tree. This port's 70-gun roster is still being built out across several
 * parallel content-wave packages (`docs/phase3/guns_and_ammo.md`'s Content Wave work-split) - as of
 * this package landing, a majority of guns exist as real registered items
 * ({@code GunPistolItems}/{@code GunRifleItems}/{@code GunShotgunItems}/{@code GunLauncherItems}), but
 * several CE-referenced guns ({@code gun_drill}, {@code gun_lasrifle}, {@code gun_tesla_cannon},
 * {@code gun_flamer}(+{@code _topaz}), {@code gun_quadro}, {@code gun_fatman}, {@code gun_tau},
 * {@code gun_laser_pistol}(+2 variants), {@code gun_n_i_4_n_i}, {@code gun_aberrator}(+{@code _eott}),
 * {@code gun_panzerschreck}, {@code gun_charge_thrower}, {@code gun_missile_launcher}) do not exist as
 * registered items yet. Rather than hardcode a compile-time dependency on those not-yet-existing
 * content classes (or split this file's single coherent association table across future packages),
 * every gun below is looked up by its exact CE-matching registry path via {@link #gun(String)}
 * ({@code BuiltInRegistries.ITEM.getOptional(...)}, the same forward-reference-safe lookup pattern
 * already used elsewhere in this port - e.g. {@code ItemCustomMissile}/{@code HazardSystem}) - a gun
 * that is not registered yet simply contributes no association (silently skipped, not an error), and
 * automatically activates the moment that gun's content package registers it under that exact path.
 * No code change is needed here once the remaining guns land.
 * <p>
 * <b>One real content gap, not a forward reference</b>: CE's caliber-swap tables need a {@code p45}
 * (.45 ACP) {@code BulletConfig} family (CE: {@code XFactory45.p45_sp/p45_fmj/p45_jhp/p45_ap/p45_du}) -
 * unlike the gun-lookup gaps above, this is genuinely missing *ammo content* (Package D), not just an
 * unregistered item this file can forward-reference by name (a {@code BulletConfig} array literal
 * needs the actual constants to exist at compile time). The 5 {@code P45}-caliber
 * {@code WeaponModDefinition}s CE declares (on {@code gun_henry}/{@code gun_greasegun}/
 * {@code gun_uzi}/{@code gun_uzi_akimbo}/{@code gun_lag}) are skipped entirely below rather than
 * registered with an empty ammo list - see {@link #init()}'s inline comment at that exact spot.
 */
public class XWeaponModManager {

    private static final Map<ResourceLocation, IWeaponMod> REGISTRY = new LinkedHashMap<>();
    /** Mapping of mod items to mod definitions. */
    public static final Map<ComparableStack, WeaponModDefinition> stackToMod = new HashMap<>();
    /** Map for turning individual mods back into their item form, used when displaying/uninstalling mods. */
    public static final Map<IWeaponMod, ItemStack> modToStack = new HashMap<>();

    /** Called by {@link WeaponModBase}'s constructor - not for direct use. */
    static void register(ResourceLocation id, IWeaponMod mod) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicate WeaponMod id: " + id);
        }
        REGISTRY.put(id, mod);
    }

    @Nullable
    public static IWeaponMod byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /** Forward-reference-safe gun lookup - see class javadoc. */
    @Nullable
    private static Item gun(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(null);
    }

    private static Item[] guns(String... paths) {
        List<Item> out = new ArrayList<>(paths.length);
        for (String path : paths) {
            Item item = gun(path);
            if (item != null) out.add(item);
        }
        return out.toArray(new Item[0]);
    }

    /** The mod items installed on {@code stack}'s config index {@code cfg}, for tooltip display. CE {@code getUpgradeItems}. */
    public static List<ItemStack> getUpgradeItems(ItemStack stack, int cfg) {
        List<String> ids = GunDataComponents.getIndexed(stack, WeaponModDataComponents.MOD_LISTS, cfg, List.<String>of());
        if (ids.isEmpty()) return List.of();
        List<ItemStack> out = new ArrayList<>(ids.size());
        for (String idStr : ids) {
            IWeaponMod mod = REGISTRY.get(ResourceLocation.parse(idStr));
            if (mod == null) continue;
            ItemStack forMod = modToStack.get(mod);
            if (forMod != null) out.add(forMod.copy());
        }
        return out;
    }

    public static boolean hasUpgrade(ItemStack stack, int cfg, ResourceLocation id) {
        String needle = id.toString();
        for (String idStr : GunDataComponents.getIndexed(stack, WeaponModDataComponents.MOD_LISTS, cfg, List.<String>of())) {
            if (idStr.equals(needle)) return true;
        }
        return false;
    }

    private static Object prevMagType;
    private static int prevMagCount;
    private static boolean changedMagState = false;

    public static void changedMagState() {
        changedMagState = true;
    }

    /** Saves the state on receiver 0 so that if the mag changes through upgrading, the state may potentially be restored, if compatible. */
    private static void saveMagState(ItemStack stack, int cfg) {
        IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
        prevMagType = mag.getType(stack, null);
        prevMagCount = mag.getAmount(stack, null);
    }

    /*
     * TODO: as soon as there's guns that use more receivers, handle those as well - arising problem:
     * assume there's three receivers, 0, 1, 2, and receiver 1 is removed by pulling a weapon mod. The
     * previous states of receivers 0 and 2 would need to be mapped to the new receivers 0 and 1.
     * Proposed solution: order can be expected the same, simply check both arrays side by side and
     * skip an index on either one if that one's type doesn't match. There may be edge cases where
     * this doesn't work, especially with a ton of receivers, but for a common case of an SMG + GL
     * this should work just fine. (Verbatim from CE - not yet reachable in this port since no ported
     * gun uses more than 1 receiver either.)
     */
    private static void restoreMagState(ItemStack stack, int cfg) {
        if (!changedMagState) return;
        changedMagState = false;

        IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
        if (Objects.equals(mag.getType(stack, null), prevMagType)) {
            mag.setAmount(stack, Mth.clamp(prevMagCount, 0, mag.getCapacity(stack)));
        } else {
            mag.setAmount(stack, 0);
        }
    }

    /**
     * Saves the mag state on receiver 0, uninstalls all existing mods to ensure there's no double
     * install calls, then installs the mods. If a mag state change has been reported, the mag on
     * receiver 0 is validated: if the type is still the same, the amount is restored, otherwise the
     * mag is cleared.
     */
    public static void install(ItemStack stack, int cfg, ItemStack... mods) {
        List<IWeaponMod> toInstall = new ArrayList<>();
        ComparableStack gun = new ComparableStack(stack);
        saveMagState(stack, cfg);
        // we need to always clear things, so existing mods aren't installed twice, i.e. enchantment levels applied twice
        uninstall(stack, cfg);

        for (ItemStack mod : mods) {
            if (mod == null || mod.isEmpty()) continue;
            ComparableStack comp = new ComparableStack(mod);
            WeaponModDefinition def = stackToMod.get(comp);
            if (def != null) {
                IWeaponMod forGun = def.modByGun.get(gun);
                if (forGun != null) toInstall.add(forGun); // since this code only runs for upgrading, we can just indexOf because who cares
                else {
                    forGun = def.modByGun.get(null);
                    if (forGun != null) toInstall.add(forGun);
                }
            }
        }
        if (toInstall.isEmpty()) return;
        toInstall.sort(MOD_SORTER);
        List<String> ids = new ArrayList<>(toInstall.size());
        for (IWeaponMod mod : toInstall) ids.add(mod.getId().toString());
        GunDataComponents.updateIndexed(stack, WeaponModDataComponents.MOD_LISTS, cfg, List.<String>of(), old -> List.copyOf(ids));
        restoreMagState(stack, cfg);
    }

    /** Wipes all mods from the gun. */
    public static void uninstall(ItemStack stack, int cfg) {
        GunDataComponents.updateIndexed(stack, WeaponModDataComponents.MOD_LISTS, cfg, List.<String>of(), old -> List.<String>of());
    }

    public static void onInstallStack(ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return;
        newMod.onInstall(gun, mod, cfg);
    }

    public static void onUninstallStack(ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return;
        newMod.onUninstall(gun, mod, cfg);
    }

    @Nullable
    public static IWeaponMod modFromStack(@Nullable ItemStack gun, @Nullable ItemStack mod, int cfg) {
        if (gun == null || gun.isEmpty() || mod == null || mod.isEmpty()) return null;
        WeaponModDefinition def = stackToMod.get(new ComparableStack(mod));
        if (def == null) return null;
        IWeaponMod newMod = def.modByGun.get(new ComparableStack(gun).makeSingular()); // shift clicking causes the gun to have stack size 0!
        if (newMod == null) newMod = def.modByGun.get(null);
        return newMod;
    }

    public static boolean isApplicable(ItemStack gun, ItemStack mod, int cfg, boolean checkMutex) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return false; // if there's just no mod applicable

        if (checkMutex) {
            for (String idStr : GunDataComponents.getIndexed(gun, WeaponModDataComponents.MOD_LISTS, cfg, List.<String>of())) {
                IWeaponMod iMod = REGISTRY.get(ResourceLocation.parse(idStr));
                if (iMod == null) continue;
                for (String mutex0 : newMod.getSlots()) {
                    for (String mutex1 : iMod.getSlots()) {
                        if (mutex0.equals(mutex1)) return false; // if any of the mod's slots are already taken
                    }
                }
            }
        }

        return true; // yippie!
    }

    public static final Comparator<IWeaponMod> MOD_SORTER = (o1, o2) -> o2.getModPriority() - o1.getModPriority();

    /**
     * Scrapes all upgrades, iterates over them and evaluates the given value. The parent (i.e. holder
     * of the base value) is passed for context (so upgrades can differentiate primary and secondary
     * receivers, for example). An empty stack causes the base value to be returned unmodified.
     */
    public static <T> T eval(T base, ItemStack stack, String key, Object parent, int cfg) {
        if (stack == null || stack.isEmpty()) return base;

        List<String> ids = GunDataComponents.getIndexed(stack, WeaponModDataComponents.MOD_LISTS, cfg, List.<String>of());
        if (ids.isEmpty()) return base;

        for (String idStr : ids) {
            IWeaponMod mod = REGISTRY.get(ResourceLocation.parse(idStr));
            if (mod != null) base = mod.eval(base, stack, key, parent);
        }

        return base;
    }

    public static class WeaponModDefinition {

        /** Holds the weapon mod handlers for each given gun. Key null refers to mods that apply to ALL guns that are otherwise not included. */
        public final Map<ComparableStack, IWeaponMod> modByGun = new HashMap<>();
        public final ItemStack stack;

        public WeaponModDefinition(ItemStack stack) {
            this.stack = stack;
            stackToMod.put(new ComparableStack(stack), this);
        }

        public WeaponModDefinition(WeaponModItems.ModGeneric num) {
            this(new ItemStack(WeaponModItems.generic(num).get()));
        }

        public WeaponModDefinition(WeaponModItems.ModSpecial num) {
            this(new ItemStack(WeaponModItems.special(num).get()));
        }

        public WeaponModDefinition(WeaponModItems.ModCaliber num) {
            this(new ItemStack(WeaponModItems.caliber(num).get()));
        }

        public WeaponModDefinition addMod(ItemStack gun, IWeaponMod mod) { return addMod(new ComparableStack(gun), mod); }

        /**
         * {@code null} is silently skipped (a not-yet-registered gun, see the enclosing class's own
         * forward-reference javadoc note) rather than being wrapped into a
         * {@code ComparableStack(null)} placeholder key: such a key would never be looked up by
         * {@link #modFromStack} (a real gun's stack always wraps a real, non-null {@code Item}) and
         * would only ever collide with sibling missing-gun placeholders within the same
         * {@link WeaponModDefinition} (silently overwriting each other) while also tripping
         * {@code ComparableStack}'s own "null item, likely a bug" warning log every time its
         * {@code hashCode()} is computed.
         */
        public WeaponModDefinition addMod(@Nullable Item gun, IWeaponMod mod) {
            if (gun == null) return this;
            return addMod(new ComparableStack(gun), mod);
        }

        public WeaponModDefinition addMod(Item[] guns, IWeaponMod mod) {
            for (Item item : guns) addMod(item, mod);
            return this;
        }

        public WeaponModDefinition addMod(@Nullable ComparableStack gun, IWeaponMod mod) {
            modByGun.put(gun, mod);
            modToStack.put(mod, stack);
            if (gun != null && gun.item instanceof ItemGunBaseNT nt) {
                ComparableStack comp = new ComparableStack(stack);
                if (!nt.recognizedMods.contains(comp)) nt.recognizedMods.add(comp);
            }
            return this;
        }

        public WeaponModDefinition addDefault(IWeaponMod mod) {
            return addMod((ComparableStack) null, mod);
        }
    }

    /** Assigns the {@link IWeaponMod} instances to items. Call once, from mod-bus common setup (after every {@code Item}/{@code BulletConfig} in this mod has registered) - see this task's wiring snippet for {@code CommonEvents.commonSetup}. */
    public static void init() {

        /* ==================== GENERIC (material-tier damage/durability kits) ==================== */

        new WeaponModDefinition(WeaponModItems.ModGeneric.IRON_DAMAGE).addMod(gun("gun_pepperbox"), new WeaponModGenericDamage("generic_iron_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.IRON_DURA).addMod(gun("gun_pepperbox"), new WeaponModGenericDurability("generic_iron_dura"));

        Item[] steelGuns = guns(
                "gun_light_revolver", "gun_light_revolver_atlas",
                "gun_henry", "gun_henry_lincoln",
                "gun_greasegun",
                "gun_maresleg", "gun_maresleg_akimbo",
                "gun_flaregun");
        Item[] duraGuns = guns(
                "gun_am180",
                "gun_liberator",
                "gun_congolake",
                "gun_flamer", "gun_flamer_topaz");
        Item[] deshGuns = guns(
                "gun_heavy_revolver",
                "gun_carbine",
                "gun_uzi", "gun_uzi_akimbo",
                "gun_spas12",
                "gun_panzerschreck");
        Item[] wsteelGuns = guns(
                "gun_star_f", "gun_star_f_akimbo",
                "gun_g3", "gun_g3_zebra",
                "gun_mk108",
                "gun_chemthrower");
        Item[] ferroGuns = guns(
                "gun_amat",
                "gun_m2",
                "gun_autoshotgun", "gun_autoshotgun_shredder",
                "gun_quadro");
        Item[] tcalloyGuns = guns(
                "gun_lag",
                "gun_minigun",
                "gun_missile_launcher",
                "gun_tesla_cannon");
        Item[] bigmtGuns = guns(
                "gun_laser_pistol", "gun_laser_pistol_pew_pew",
                "gun_stg77",
                "gun_fatman",
                "gun_tau");
        Item[] bronzeGuns = guns("gun_lasrifle");

        new WeaponModDefinition(WeaponModItems.ModGeneric.STEEL_DAMAGE).addMod(steelGuns, new WeaponModGenericDamage("generic_steel_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.STEEL_DURA).addMod(steelGuns, new WeaponModGenericDurability("generic_steel_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.DURA_DAMAGE).addMod(duraGuns, new WeaponModGenericDamage("generic_dura_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.DURA_DURA).addMod(duraGuns, new WeaponModGenericDurability("generic_dura_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.DESH_DAMAGE).addMod(deshGuns, new WeaponModGenericDamage("generic_desh_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.DESH_DURA).addMod(deshGuns, new WeaponModGenericDurability("generic_desh_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.WSTEEL_DAMAGE).addMod(wsteelGuns, new WeaponModGenericDamage("generic_wsteel_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.WSTEEL_DURA).addMod(wsteelGuns, new WeaponModGenericDurability("generic_wsteel_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.FERRO_DAMAGE).addMod(ferroGuns, new WeaponModGenericDamage("generic_ferro_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.FERRO_DURA).addMod(ferroGuns, new WeaponModGenericDurability("generic_ferro_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.TCALLOY_DAMAGE).addMod(tcalloyGuns, new WeaponModGenericDamage("generic_tcalloy_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.TCALLOY_DURA).addMod(tcalloyGuns, new WeaponModGenericDurability("generic_tcalloy_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.BIGMT_DAMAGE).addMod(bigmtGuns, new WeaponModGenericDamage("generic_bigmt_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.BIGMT_DURA).addMod(bigmtGuns, new WeaponModGenericDurability("generic_bigmt_dura"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.BRONZE_DAMAGE).addMod(bronzeGuns, new WeaponModGenericDamage("generic_bronze_damage"));
        new WeaponModDefinition(WeaponModItems.ModGeneric.BRONZE_DURA).addMod(bronzeGuns, new WeaponModGenericDurability("generic_bronze_dura"));

        /* ==================== SPECIAL (named/unique attachments) ==================== */

        new WeaponModDefinition(WeaponModItems.ModSpecial.SPEEDLOADER).addMod(gun("gun_liberator"), new WeaponModLiberatorSpeedloader("speedloader"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SILENCER).addMod(
                guns("gun_am180", "gun_uzi", "gun_uzi_akimbo", "gun_star_f", "gun_star_f_akimbo", "gun_g3", "gun_amat"),
                new WeaponModSilencer("silencer"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SCOPE).addMod(
                guns("gun_heavy_revolver", "gun_g3", "gun_mas36", "gun_charge_thrower"),
                new WeaponModScope("scope"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SAW)
                .addMod(guns("gun_maresleg", "gun_double_barrel"), new WeaponModSawedOff("sawed_off"))
                .addMod(gun("gun_panzerschreck"), new WeaponModPanzerschreckSawedOff("no_shield"))
                .addMod(guns("gun_g3", "gun_g3_zebra"), new WeaponModG3SawedOff("no_stock"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.GREASEGUN).addMod(gun("gun_greasegun"), new WeaponModGreasegun("greasegun_clean"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SLOWDOWN).addMod(guns("gun_minigun", "gun_minigun_dual"), new WeaponModSlowdown("minigun_slowdown"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SPEEDUP)
                .addMod(guns("gun_minigun", "gun_minigun_dual"), new WeaponModMinigunSpeedup("minigun_speedup"))
                .addMod(guns("gun_autoshotgun", "gun_autoshotgun_shredder", "gun_mk108"), new WeaponModShredderSpeedup("shredder_speedup"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.CHOKE).addMod(
                guns("gun_pepperbox", "gun_maresleg", "gun_double_barrel", "gun_liberator", "gun_spas12", "gun_autoshotgun_sexy", "gun_autoshotgun_heretic"),
                new WeaponModChoke("choke"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.FURNITURE_GREEN).addMod(gun("gun_g3"), new WeaponModPolymerFurniture("furniture_green"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.FURNITURE_BLACK).addMod(gun("gun_g3"), new WeaponModPolymerFurniture("furniture_black"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.BAYONET)
                .addMod(gun("gun_mas36"), new WeaponModMASBayonet("mas_bayonet"))
                .addMod(gun("gun_carbine"), new WeaponModCarbineBayonet("carbine_bayonet"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.STACK_MAG).addMod(
                guns("gun_greasegun", "gun_uzi", "gun_uzi_akimbo", "gun_aberrator", "gun_aberrator_eott"),
                new WeaponModStackMag("stack_mag"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SKIN_SATURNITE).addMod(guns("gun_uzi", "gun_uzi_akimbo"), new WeaponModUziSaturnite("uzi_saturnite"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.LAS_SHOTGUN).addMod(gun("gun_lasrifle"), new WeaponModLasShotgun("las_shotgun"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.LAS_CAPACITOR).addMod(gun("gun_lasrifle"), new WeaponModLasCapacitor("las_capacitor"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.LAS_AUTO).addMod(gun("gun_lasrifle"), new WeaponModLasAuto("las_auto"));

        new WeaponModDefinition(WeaponModItems.ModSpecial.NICKEL).addMod(gun("gun_n_i_4_n_i"), new WeaponModNickel("ni4ni_nickel", "COIN1"));
        new WeaponModDefinition(WeaponModItems.ModSpecial.DOUBLOONS).addMod(gun("gun_n_i_4_n_i"), new WeaponModNickel("ni4ni_doubloons", "COIN2"));

        new WeaponModDefinition(WeaponModItems.ModSpecial.DRILL_HSS).addMod(gun("gun_drill"),
                new WeaponModDrill("drill_hss").damage(1.25F).dt(3F).pierce(0.15F).harvest(3));
        new WeaponModDefinition(WeaponModItems.ModSpecial.DRILL_WEAPONSTEEL).addMod(gun("gun_drill"),
                new WeaponModDrill("drill_wsteel").damage(1.5F).dt(5F).pierce(0.2F).aoe(2).harvest(3));
        new WeaponModDefinition(WeaponModItems.ModSpecial.DRILL_TCALLOY).addMod(gun("gun_drill"),
                new WeaponModDrill("drill_tcalloy").damage(2F).dt(7.5F).pierce(0.2F).reach(1.5).aoe(2).harvest(4));
        new WeaponModDefinition(WeaponModItems.ModSpecial.DRILL_SATURNITE).addMod(gun("gun_drill"),
                new WeaponModDrill("drill_saturnite").damage(3F).dt(10F).pierce(0.25F).reach(2).aoe(2).harvest(5));
        new WeaponModDefinition(WeaponModItems.ModSpecial.ENGINE_DIESEL).addMod(gun("gun_drill"), new WeaponModEngine("engine_diesel").mag(WeaponModEngine.ENGINE_DIESEL).delay(15));
        new WeaponModDefinition(WeaponModItems.ModSpecial.ENGINE_AVIATION).addMod(gun("gun_drill"), new WeaponModEngine("engine_aviation").mag(WeaponModEngine.ENGINE_AVIATION).delay(10));
        new WeaponModDefinition(WeaponModItems.ModSpecial.ENGINE_ELECTRIC).addMod(gun("gun_drill"), new WeaponModEngine("engine_electric").mag(WeaponModEngine.ENGINE_ELECTRIC).delay(15));
        new WeaponModDefinition(WeaponModItems.ModSpecial.ENGINE_TURBO).addMod(gun("gun_drill"), new WeaponModEngine("engine_turbo").mag(WeaponModEngine.ENGINE_TURBO).delay(2));
        new WeaponModDefinition(WeaponModItems.ModSpecial.MAGNET).addMod(gun("gun_drill"), new WeaponModDrillFortune("magnet", "MAGNET", 2));
        new WeaponModDefinition(WeaponModItems.ModSpecial.SIFTER).addMod(gun("gun_drill"), new WeaponModDrillFortune("sifter", "SIFTER", 1));
        new WeaponModDefinition(WeaponModItems.ModSpecial.CANISTERS).addMod(gun("gun_drill"), new WeaponModCanisters("canisters"));

        /* ==================== CALIBER (ammo-family conversion kits) ==================== */
        // CE's harvest-level args (Item.ToolMaterial.DIAMOND.ordinal() [+1/+2], a legacy 1.12 ordinal)
        // are replaced above with plain ascending ints (3/4/5) - the actual 1.21 harvest-level
        // consumer (a real block-breaking BlockState#requiresCorrectToolForDrops check) is Package D
        // content (gun_drill's own onFire lambda, not yet ported) that doesn't exist to compare
        // against yet; these ints are a placeholder ordering, not a verified final harvest tier.

        BulletConfig[] p9 = {XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap};
        BulletConfig[] p22 = {XFactory22lr.p22_sp, XFactory22lr.p22_fmj, XFactory22lr.p22_jhp, XFactory22lr.p22_ap};
        BulletConfig[] m357 = {XFactory357.m357_sp, XFactory357.m357_fmj, XFactory357.m357_jhp, XFactory357.m357_ap, XFactory357.m357_express};
        BulletConfig[] m44 = {XFactory44.m44_sp, XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express};
        BulletConfig[] r556 = {XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp, XFactory556mm.r556_ap};
        BulletConfig[] r762 = {XFactory762mm.r762_sp, XFactory762mm.r762_fmj, XFactory762mm.r762_jhp, XFactory762mm.r762_ap, XFactory762mm.r762_du, XFactory762mm.r762_he};
        BulletConfig[] bmg50 = {XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_he};

        new WeaponModDefinition(WeaponModItems.ModCaliber.P9)
                .addMod(gun("gun_henry"), new WeaponModCaliber("caliber_p9_henry", 28, 10F, p9))
                .addMod(gun("gun_star_f"), new WeaponModCaliber("caliber_p9_star_f", 12, 15F, p9))
                .addMod(gun("gun_star_f_akimbo"), new WeaponModCaliber("caliber_p9_star_f_akimbo", 12, 15F, p9));
        // P45 (.45 ACP): skipped entirely - genuinely missing ammo content, not a forward reference. See class javadoc.
        new WeaponModDefinition(WeaponModItems.ModCaliber.P22)
                .addMod(gun("gun_henry"), new WeaponModCaliber("caliber_p22_henry", 28, 10F, p22))
                .addMod(gun("gun_uzi"), new WeaponModCaliber("caliber_p22_uzi", 40, 3F, p22))
                .addMod(gun("gun_uzi_akimbo"), new WeaponModCaliber("caliber_p22_uzi_akimbo", 40, 3F, p22));
        new WeaponModDefinition(WeaponModItems.ModCaliber.M357)
                .addMod(gun("gun_henry"), new WeaponModCaliber("caliber_m357_henry", 20, 10F, m357))
                .addMod(gun("gun_lag"), new WeaponModCaliber("caliber_m357_lag", 15, 25F, m357));
        new WeaponModDefinition(WeaponModItems.ModCaliber.M44)
                .addMod(gun("gun_lag"), new WeaponModCaliber("caliber_m44_lag", 13, 25F, m44));
        new WeaponModDefinition(WeaponModItems.ModCaliber.R556)
                .addMod(gun("gun_henry"), new WeaponModCaliber("caliber_r556_henry", 10, 10F, r556))
                .addMod(gun("gun_carbine"), new WeaponModCaliber("caliber_r556_carbine", 20, 15F, r556))
                .addMod(guns("gun_minigun", "gun_minigun_dual"), new WeaponModCaliber("caliber_r556_minigun", 0, 6F, r556));
        new WeaponModDefinition(WeaponModItems.ModCaliber.R762)
                .addMod(gun("gun_henry"), new WeaponModCaliber("caliber_r762_henry", 8, 10F, r762))
                .addMod(gun("gun_g3"), new WeaponModCaliber("caliber_r762_g3", 24, 5F, r762));
        new WeaponModDefinition(WeaponModItems.ModCaliber.BMG50)
                .addMod(gun("gun_henry"), new WeaponModCaliber("caliber_bmg50_henry", 5, 10F, bmg50))
                .addMod(guns("gun_minigun", "gun_minigun_dual"), new WeaponModCaliber("caliber_bmg50_minigun", 0, 6F, bmg50));

        // NOTE: every gun(...)/guns(...) lookup above only finds a gun that BuiltInRegistries.ITEM
        // already knows about *when init() runs*. This method is called from CommonEvents.commonSetup
        // inside event.enqueueWork(...) - strictly after every RegisterEvent has fired, which in turn
        // requires ModItems.register(modEventBus) to have already called
        // GunPistolItems/GunRifleItems/GunShotgunItems/GunLauncherItems.registerAll() (each class's
        // real registration work happens in its own static initializer, forced to run by that call)
        // *before* its own ITEMS.register(modEventBus) line - see this task's wiring snippet for
        // ModItems.java, which is the single load-bearing ordering guarantee this whole forward-
        // reference scheme depends on.
    }
}
