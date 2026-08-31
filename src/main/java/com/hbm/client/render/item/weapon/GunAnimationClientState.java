package com.hbm.client.render.item.weapon;

import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna;
import com.hbm.weapon.anim.HbmAnimationType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Client-only "which {@link BusAnimationSedna} is currently playing, and since when" registry -
 * port of CE's {@code com.hbm.render.anim.sedna.HbmAnimationsSedna} (131 lines, read in full),
 * adapted per {@code docs/phase5/weapon_gun_rendering_animloader.md}'s explicit recommendation
 * ("Two competing designs" section): kept as CE's own client-only
 * {@code hotbar[slot][rail]} array (not folded into this port's per-{@code ItemStack} Data
 * Component design, {@code GunStateComponent.lastAnim}/{@code animTimer}) specifically so the
 * render-time clock stays a client-local {@link System#currentTimeMillis()} captured once when
 * {@link com.hbm.packet.toclient.GunAnimationPayload} arrives - see that class's
 * {@code handleClient} for the write side of this registry - sidestepping the doc's flagged
 * "20/sec full-{@code ItemStack} resync packet" risk of trusting the synced {@code animTimer}
 * field as a moment-to-moment interpolation clock.
 *
 * <h2>1.21.1 API deltas from CE (mechanical only, zero behavioral change)</h2>
 * <ul>
 *   <li>{@code EntityPlayer}/{@code EnumHand} -> {@link LocalPlayer}/{@code InteractionHand}
 *       (unused here - CE's own handler only ever reads {@code getHeldItemMainhand()}, never
 *       offhand, so this port does the same, matching CE's real behavior over its own record's
 *       theoretical hand-encoding capability).</li>
 *   <li>{@code stack.getItem().getTranslationKey()} -&gt; {@code stack.getItem().getDescriptionId()}
 *       (1.21.1 rename, same semantic - the item's translation key, used purely as a same-gun-type
 *       identity check exactly like CE's own "you can still 'trick' the system" comment describes).</li>
 *   <li>{@code GlStateManager.translate/rotate/scale} -&gt; {@link PoseStack}/{@link Axis} calls in
 *       {@link #applyRelevantTransformation} - a 1:1 structural swap, same rotation-order swizzle
 *       logic, confirmed against {@code upstream/neo-edition}'s real, compiling
 *       {@code com.hbm.render.anim.HbmAnimations.applyRelevantTransformation} for the
 *       {@code PoseStack}/{@code Axis} idiom (that class's own animation engine is the legacy,
 *       non-Sedna one - only the {@code GlStateManager}-&gt;{@code PoseStack} API shape is
 *       cross-checked here, not its behavior, per this task's ground rules).</li>
 * </ul>
 */
public final class GunAnimationClientState {

    private GunAnimationClientState() {
    }

    /**
     * 9 hotbar slots x 8 parallel "rails" (config/gun-mode indices) per slot - CE's own comment,
     * preserved: "in flans mod and afaik also MW, there's an issue that there is only one single
     * animation timer for each client... my approach adds 9 timers, one for every inventory slot.
     * you can still 'trick' the system by putting a weapon into a different slot while an animation
     * is playing, though this will cancel the animation entirely."
     */
    public static final Animation[][] hotbar = new Animation[9][8];

    /** One playing-animation record - CE's {@code HbmAnimationsSedna.Animation} inner class, ported field-for-field. */
    public static final class Animation {
        /** The item's translation key at the moment this animation started - if the held stack's key no longer matches, the animation is considered stale (see {@link #getRelevantAnim(int)}). */
        public final String key;
        public final long startMillis;
        public final BusAnimationSedna animation;
        /** If set, don't cancel this animation when the timer ends, instead wait for the next to start. Stored for parity; this port's {@link #getRelevantTransformation} does not itself branch on it (matches the two CE methods this class ports - neither reads it either). */
        public final boolean holdLastFrame;
        public final HbmAnimationType type;

        public Animation(String key, long startMillis, BusAnimationSedna animation, HbmAnimationType type, boolean holdLastFrame) {
            this.key = key;
            this.startMillis = startMillis;
            this.animation = animation;
            this.type = type;
            this.holdLastFrame = holdLastFrame;
        }
    }

    @Nullable
    public static Animation getRelevantAnim() {
        return getRelevantAnim(0);
    }

    @Nullable
    public static Animation getRelevantAnim(int index) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;

        int slot = player.getInventory().selected;
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty())
            return null;

        if (slot < 0 || slot > 8) { // for freak-of-nature hotbars, probably won't work right but at least it doesn't crash
            slot = Math.abs(slot) % 9;
        }

        if (index < 0 || index >= hotbar[slot].length) return null;

        Animation anim = hotbar[slot][index];
        if (anim == null)
            return null;

        if (anim.key.equals(stack.getItem().getDescriptionId())) {
            return anim;
        }

        return null;
    }

    /** Default/neutral transform - identity position/rotation/scale, XYZ rotation order - returned whenever no animation is currently playing on this rail. */
    private static double[] defaultTransform() {
        return new double[]{
                0, 0, 0, // position
                0, 0, 0, // rotation
                1, 1, 1, // scale
                0, 0, 0, // offset
                0, 1, 2, // XYZ order
        };
    }

    public static double[] getRelevantTransformation(String bus) {
        return getRelevantTransformation(bus, 0);
    }

    public static double[] getRelevantTransformation(String bus, int index) {
        Animation anim = getRelevantAnim(index);

        if (anim != null) {
            BusAnimationSedna buses = anim.animation;
            int millis = (int) (System.currentTimeMillis() - anim.startMillis);

            BusAnimationSequenceSedna seq = buses.getBus(bus);

            if (seq != null) {
                double[] trans = seq.getTransformation(millis);
                if (trans != null)
                    return trans;
            }
        }

        return defaultTransform();
    }

    public static void applyRelevantTransformation(PoseStack poseStack, String bus) {
        applyRelevantTransformation(poseStack, bus, 0);
    }

    /**
     * Mutates {@code poseStack} in place with the sampled transform for {@code bus} - the
     * {@link PoseStack}/{@link Axis} equivalent of CE's
     * {@code GlStateManager.translate/rotate/scale} sequence, same rotation-order swizzle
     * ({@code rot[0..2]} indexing back into the sampled Euler angles) and same
     * subtract-then-scale offset order.
     */
    public static void applyRelevantTransformation(PoseStack poseStack, String bus, int index) {
        double[] transform = getRelevantTransformation(bus, index);
        int[] rot = new int[]{(int) transform[12], (int) transform[13], (int) transform[14]};

        poseStack.translate(transform[0], transform[1], transform[2]);
        for (int i = 0; i < 3; i++) {
            Axis axis = rot[i] == 0 ? Axis.XP : (rot[i] == 1 ? Axis.YP : Axis.ZP);
            poseStack.mulPose(axis.rotationDegrees((float) transform[3 + rot[i]]));
        }
        poseStack.translate(-transform[9], -transform[10], -transform[11]);
        poseStack.scale((float) transform[6], (float) transform[7], (float) transform[8]);
    }
}
