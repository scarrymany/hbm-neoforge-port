package com.hbm.weapon.anim;

/**
 * Marker interface for a weapon family's animation-trigger vocabulary - a pure enum tag with zero
 * client-rendering dependency (no {@code BusAnimation} lookup, no GL/{@code Minecraft} reference).
 * See {@code docs/phase3/weapon_animation_hooks.md}'s headline finding: CE actually has two
 * unrelated "animation" systems, and only the hand-rolled keyframe one
 * ({@code com.hbm.render.anim.BusAnimation}/{@code .sedna.BusAnimationSedna}, not the Collada/GL
 * {@code com.hbm.animloader} package) is a Phase 3 dependency at all - deciding *that* an
 * animation should start (this vocabulary + the network trigger) is server-authoritative gun/tool
 * logic; sampling a {@code BusAnimation} every frame and feeding it into render-time transforms is
 * Phase 5.
 * <p>
 * Mirrors CE's Sedna-era {@code com.hbm.render.anim.sedna.AnimationEnums.AnimationType} marker,
 * which already proved guns and melee tools can share one trigger vocabulary and one network
 * payload shape instead of each weapon family inventing its own - the research report explicitly
 * recommends copying this marker-interface pattern from Phase 3 onward rather than porting the
 * legacy single-enum {@code com.hbm.render.anim.HbmAnimations.AnimType} it eventually superseded
 * in CE.
 * <p>
 * Implementors: {@link GunAnimationType} (fired by the gun framework's fire()/reload() cycle - not
 * yet ported, see the future {@code gun_framework_core} package) and {@link ToolAnimationType}
 * (fired by melee tools such as the future {@code ItemChainsaw} port). Both are carried identically
 * by {@link com.hbm.packet.toclient.GunAnimationPayload}'s wire format (an ordinal into whichever
 * concrete enum the sending code knows it is dispatching); the receiving side determines which
 * enum family an ordinal belongs to from the held item's own type, exactly as CE's own
 * {@code GunAnimationPacketSedna} handler does (it checks {@code instanceof ItemGunBaseNT} before
 * ever interpreting the ordinal) - so no family discriminator byte needs to travel on the wire.
 */
public interface HbmAnimationType {
}
