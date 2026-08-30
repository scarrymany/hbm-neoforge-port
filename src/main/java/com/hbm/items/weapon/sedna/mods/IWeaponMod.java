package com.hbm.items.weapon.sedna.mods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Direct port of CE's {@code com.hbm.items.weapon.sedna.mods.IWeaponMod} (16 lines) - the per-mod
 * eval-hook interface every concrete {@code WeaponMod*} effect class implements (via
 * {@link WeaponModBase}). See {@code docs/phase3/gun_framework.md}'s Package C section, read in full.
 * <p>
 * {@link #getId()} is this port's own addition (CE has no equivalent method - it kept a bidirectional
 * {@code HashBiMap<Integer, IWeaponMod>} in {@link XWeaponModManager} instead). This port replaces
 * CE's fragile construction-order {@code int} ids (assigned via magic-number constructor arguments
 * like {@code new WeaponModSilencer(201)}, threaded through a pile of
 * {@code XWeaponModManager.ID_SILENCER = 201}-style constants with no semantic meaning) with an
 * explicit {@link ResourceLocation} key, mirroring {@code BulletConfig}'s already-committed id scheme
 * (see that class's javadoc) - every {@code WeaponMod*} instance is constructed with an explicit,
 * human-readable id string via {@link WeaponModBase}'s constructor, and that id is what actually gets
 * persisted (as a plain string, inside the {@link WeaponModDataComponents#MOD_LISTS} data component)
 * rather than a network/session-only integer.
 */
public interface IWeaponMod {

    /** This mod's registered id - see interface javadoc for why this port adds it over CE's shape. */
    ResourceLocation getId();

    /** Lower numbers get installed and therefore evaluated first. Important when multiplicative and additive bonuses are supposed to stack. */
    int getModPriority();

    String[] getSlots();

    /**
     * The meat and bones of the upgrade eval. Requires the base value, the held gun, the value's
     * identifier and the yet-unmodified parent (i.e. if the value is part of a {@code Receiver}, that
     * receiver).
     */
    <T> T eval(T base, ItemStack gun, String key, Object parent);

    default void onInstall(ItemStack gun, ItemStack mod, int index) {
    }

    default void onUninstall(ItemStack gun, ItemStack mod, int index) {
    }
}
