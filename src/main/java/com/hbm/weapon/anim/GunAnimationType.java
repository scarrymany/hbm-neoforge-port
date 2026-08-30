package com.hbm.weapon.anim;

/**
 * Gun animation-trigger vocabulary. Ported from CE's
 * {@code com.hbm.render.anim.sedna.AnimationEnums.GunAnimation} (the Sedna-era superset of the
 * legacy {@code com.hbm.render.anim.HbmAnimations.AnimType}'s six values -
 * {@code RELOAD, CYCLE, ALT_CYCLE, SPINUP, SPINDOWN, EQUIP}). This port deliberately adopts the
 * wider Sedna vocabulary rather than the legacy enum for both gun frameworks, per
 * {@code docs/phase3/weapon_animation_hooks.md}'s own recommendation to copy the marker-interface
 * pattern "even for the legacy/simple gun path" - whichever gun-framework package lands first can
 * use as much or as little of this vocabulary as its own {@code GunConfiguration.animations} map
 * actually populates (CE's own trigger sites already treat "no animation configured for this type"
 * as "send nothing", see {@link com.hbm.packet.toclient.GunAnimationPayload#triggerGunAnimation}).
 * <p>
 * CE's {@code RELOAD_EMPTY} value is intentionally omitted: CE itself marks it {@code @Deprecated}
 * and its own packet handler falls back {@code RELOAD_EMPTY -> RELOAD} whenever no dedicated
 * animation is configured, i.e. it is not a distinct trigger, just a legacy alias of {@link #RELOAD}.
 * Likewise {@code CYCLE_EMPTY} (the "final shot in the magazine" variant) is omitted here - CE's own
 * handler falls it back to {@link #CYCLE} the same way it falls back {@link #ALT_CYCLE}, and no
 * currently-scoped Phase 3 package depends on the distinction; a future package that needs it can
 * add the value without touching the wire format (it is carried as a plain ordinal).
 * <p>
 * Pure data - zero client-rendering dependency, confirmed Phase-3-safe (see {@link HbmAnimationType}).
 */
public enum GunAnimationType implements HbmAnimationType {
    /** Either a full reload or the start of one. */
    RELOAD,
    /** Plays once per individual round loaded (shotgun-style single-round reloads). */
    RELOAD_CYCLE,
    /** Transition from a {@link #RELOAD_CYCLE} sequence back to idle. */
    RELOAD_END,
    /** Plays on every successful firing cycle. */
    CYCLE,
    /** Plays when trying to fire with no round available (dry-fire click, no shot spawned). */
    CYCLE_DRY,
    /** Plays on alt-fire cycles. */
    ALT_CYCLE,
    /** Plays on action-start (e.g. minigun-style spin-up before the first shot). */
    SPINUP,
    /** Plays on action-end (spin-down once firing stops). */
    SPINDOWN,
    /** Plays when the weapon is drawn/equipped. */
    EQUIP,
    /** Plays while inspecting the weapon. */
    INSPECT,
    /** Plays while the weapon is jammed. */
    JAMMED
}
