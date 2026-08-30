package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunDataComponents;
import com.hbm.items.weapon.sedna.MagState;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.UnaryOperator;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mags.IMagazine} (73 lines) - the reload contract.
 * The magazine simply provides the receiver it's attached to with ammo; the receiver does not care
 * where it comes from. It is the mag's own responsibility to handle reloading, any type restrictions,
 * and belt-like "magless" action.
 * <p>
 * {@code inventory} is {@code null} for a mob-held gun (CE's {@code EntityAIFireGun} calls every one
 * of these with a null {@code IInventory}) - every concrete mag flavor must degrade gracefully rather
 * than NPE, exactly as CE's own implementations do (see each concrete class's javadoc for its own
 * null-inventory behavior).
 * <p>
 * Not ported from CE: {@code getCasing(ItemStack, IInventory)} (returns a {@code SpentCasing}, a
 * pure client-rendering particle-effect config with no unported dependency chain of its own - out of
 * this state-machine package's scope, see {@code BulletConfig}'s own javadoc for the analogous
 * {@code casing}/{@code SpentCasing} field it also omits). {@link #handleAmmoBag}/
 * {@link #shouldUseUpTrenchie} are ported as documented stubs (see their own javadocs) since their
 * real CE dependencies ({@code ItemCasingBag.pushCasing}, {@code ArmorTrenchmaster}) don't exist in
 * this port yet.
 */
public interface IMagazine<T> {

    /** What ammo is loaded currently. */
    T getType(ItemStack stack, @Nullable Container inventory);
    /** Sets the mag's ammo type. */
    void setType(ItemStack stack, T type);
    /** How much ammo this mag can carry. */
    int getCapacity(ItemStack stack);
    /** How much ammo is currently loaded. */
    int getAmount(ItemStack stack, @Nullable Container inventory);
    /** Sets the mag's ammo level. */
    void setAmount(ItemStack stack, int amount);
    /** Removes the specified amount from the magazine. */
    void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount);
    /** Whether a reload can even be initiated, i.e. the player has bullets to load; inventory can be null. */
    boolean canReload(ItemStack stack, @Nullable Container inventory);
    /** On the start of a reload, potentially change the mag type before the reload happens, for animation purposes. */
    void initNewType(ItemStack stack, @Nullable Container inventory);
    /** The action done at the end of one reload cycle, either loading one shell or replacing the whole mag; inventory can be null. */
    void reloadAction(ItemStack stack, @Nullable Container inventory);
    /** The stack that should be displayed for the ammo HUD. */
    ItemStack getIconForHUD(ItemStack stack, Player player);
    /** It explains itself. */
    String reportAmmoStateForHUD(ItemStack stack, Player player);
    /** When reloading, remember the amount before the reload is initiated. */
    void setAmountBeforeReload(ItemStack stack, int amount);
    /**
     * Amount of rounds before reload has started. Do note that the component sync likely arrives
     * after the animation packets (CE's own doc comment - a real client/server race this port
     * preserves the same workaround for), so for RELOAD-type animations, use the live ammo count
     * instead!
     */
    int getAmountBeforeReload(ItemStack stack);
    /** Sets amount of ammo after each reload operation. */
    void setAmountAfterReload(ItemStack stack, int amount);
    /** Cached amount of ammo after the most recent reload. */
    int getAmountAfterReload(ItemStack stack);

    /** Shared {@link MagState} read helper for concrete mag implementations, keyed by a mag's own {@code index} field (see {@link MagState}'s javadoc). */
    static MagState magState(ItemStack stack, int index) {
        return GunDataComponents.getIndexed(stack, GunDataComponents.MAG_STATES, index, MagState.EMPTY);
    }

    /** Shared {@link MagState} write helper for concrete mag implementations. */
    static void updateMagState(ItemStack stack, int index, UnaryOperator<MagState> mutator) {
        GunDataComponents.updateIndexed(stack, GunDataComponents.MAG_STATES, index, MagState.EMPTY, mutator);
    }

    /**
     * Pushes spent casings into a held {@code ItemCasingBag}, for casing-collection gameplay.
     * <b>Stubbed</b>: {@code com.hbm.items.tool.ItemCasingBag.pushCasing} does not exist in this port
     * yet (that class's own javadoc: its casing-collection inventory is deferred pending a generic
     * item-owned-inventory Menu/Screen framework). A no-op until that lands - ammo still fires and
     * consumes correctly either way, only the casing-collection side effect is skipped.
     */
    static void handleAmmoBag(@Nullable Container inventory, @Nullable BulletConfig config, int shotsFired) {
        // TODO(items-tool): wire to ItemCasingBag.pushCasing once that class's casing inventory lands.
    }

    /**
     * Whether an ammo-conserving armor set (CE: {@code ArmorTrenchmaster}, a 2-in-3 chance to not
     * consume a round) should let this shot consume ammo normally. <b>Stubbed</b>:
     * {@code com.hbm.items.armor.ArmorTrenchmaster} does not exist in this port yet - always returns
     * {@code true} (i.e. "always consume ammo normally") until that armor set lands, which is the
     * strictly-more-conservative default (a mob/player never gets free ammo it shouldn't).
     */
    static boolean shouldUseUpTrenchie(@Nullable Container inventory) {
        // TODO(items-armor): route through ArmorTrenchmaster.isTrenchMaster/hasAoS once that class exists.
        return true;
    }
}
