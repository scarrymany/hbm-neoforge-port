package com.hbm.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Generic "lockable block entity" contract, ported from the shape of CE's
 * {@code com.hbm.tileentity.machine.TileEntityLockableBase} (an abstract {@code TileEntity} base,
 * read in full from {@code upstream/hbm-ce}) but reworked as an interface rather than a base class:
 * this port's storage crate family ({@link com.hbm.blockentity.machine.CrateBlockEntity}) already
 * has its own base-class chain ({@code MachineBaseBlockEntity}) and its own javadoc explicitly notes
 * it ships with "no lock/pin mechanism" and recommends "porting the lock/pin item family in the same
 * pass" as whichever future work adds it - see that class's javadoc, and
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Part A.1 entry for
 * {@code ItemKeyPin}/{@code ItemKey}/{@code ItemLock}/{@code ItemCounterfeitKeys}, which explicitly
 * frames the target as "a generic 'lockable block entity' interface concept rather than a specific
 * machine."
 * <p>
 * <b>No concrete block entity in this port implements this interface yet</b> (confirmed by a
 * repo-wide grep before writing this file) - this is real, working, generic infrastructure with no
 * live consumer, exactly like {@link IToolable}/{@link IAnalyzable} were each before their own first
 * concrete implementor landed. The four lock/key items in {@code com.hbm.items.tool} dispatch through
 * this interface via {@code instanceof} and degrade to a harmless no-op ({@code InteractionResult.PASS})
 * against any block/tile entity that doesn't implement it - once {@code CrateBlockEntity} (or any
 * other future machine) implements {@code ILockable}, every one of those items starts working against
 * it with zero further item-side changes, matching the interface-based coupling pattern
 * {@link IToolable}/{@link IAnalyzable} already established in this port.
 * <p>
 * {@link #canAccess}/{@link #tryPick} carry CE's real default logic (pin match, or an unconditional
 * "universal key" override) as interface default methods, rather than being re-derived per
 * implementor - CE's own {@code canAccess}/{@code tryPick} lived once on the shared
 * {@code TileEntityLockableBase} class and every column/crate/door subclass inherited it unchanged;
 * default methods are this port's equivalent single-definition point. The armor-based pick-chance
 * bonus CE's {@code tryPick} applies via {@code ArmorUtil.checkArmorPiece(player, ModItems.jackt/jackt2, 2)}
 * is intentionally dropped here - those two armor items are Phase 1/3 gear content orthogonal to the
 * lock system itself, not a lock-package concern, and dropping a chance *bonus* only ever makes
 * picking a lock harder than CE, never lets a player bypass a lock CE would have blocked.
 */
public interface ILockable {

    boolean isLocked();

    /** Whether this lock target currently allows a new lock to be installed on it. CE: {@code canLock}, always {@code true} by default. */
    default boolean canLock(Player player, InteractionHand hand, Direction facing) {
        return true;
    }

    void lock();

    void unlock();

    void setPins(int pins);

    int getPins();

    void setMod(double mod);

    double getMod();

    /** Whether a counterfeit key can be cut from this lock. CE: {@code TileEntityLockableBase.cheesable}, default {@code true}. */
    default boolean isCheesable() {
        return true;
    }

    /**
     * CE: {@code TileEntityLockableBase#canAccess}. {@code universalKey} mirrors CE's
     * {@code stack.getItem() == ModItems.key_red} special case (an unconditional master key) -
     * callers pass {@code true} when the stack held is such a master key, regardless of its own
     * pins. Picking (the {@code tryPick} fallback) is left to the caller: unlike CE, this default
     * does not assume a specific lockpick-tool pairing, since none of this port's lock/key items
     * define one yet - see each item's own javadoc.
     */
    default boolean canAccess(int heldPins, boolean universalKey) {
        if (!isLocked()) return true;
        if (universalKey) return true;
        return heldPins == getPins();
    }
}
