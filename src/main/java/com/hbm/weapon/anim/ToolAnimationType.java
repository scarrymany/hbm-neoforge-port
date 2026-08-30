package com.hbm.weapon.anim;

/**
 * Melee-tool animation-trigger vocabulary. Ported from CE's
 * {@code com.hbm.render.anim.sedna.AnimationEnums.ToolAnimation}. Lets melee tools such as the
 * future {@code ItemChainsaw} port (see {@code docs/phase3/weapon_animation_hooks.md}'s "non-gun,
 * non-animloader path" finding) reuse {@link com.hbm.packet.toclient.GunAnimationPayload}'s wire
 * format and {@code HbmNetwork} registration instead of CE's much larger {@code HbmEffectNT}
 * generic effect-dispatch table (confirmed unported per Phase 2's own research, and out of scope
 * for this shared network infrastructure to build) - this directly resolves that report's
 * "ItemChainsaw/IAnimatedItem" deferred item without taking on {@code HbmEffectNT}'s full scope.
 */
public enum ToolAnimationType implements HbmAnimationType {
    /** Plays on a melee swing/attack. */
    SWING,
    /** Plays when the tool is drawn/equipped. */
    EQUIP
}
