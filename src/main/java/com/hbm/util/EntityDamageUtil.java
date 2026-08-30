package com.hbm.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Partial port of CE's {@code com.hbm.util.EntityDamageUtil} (421 lines in CE). Only the two entry
 * points the ballistics core's default hit-resolution lambdas call are ported here -
 * {@link #attackEntityFromIgnoreIFrame} and {@link #attackEntityFromNT} - not
 * {@code getMouseOver}/{@code rayTrace} (player look-vector raycasting, a melee/interaction-scope
 * concern unrelated to gun/turret ballistics) or the sound/reflection helpers CE's alternate
 * damage-pipeline branch needed only because 1.12/Forge left {@code EntityLivingBase}'s hurt-sound
 * methods {@code private}.
 * <p>
 * <b>Simplification, documented rather than silently taken</b>: CE's real {@code attackEntityFromNT}
 * dispatches to one of two from-scratch reimplementations of vanilla's entire
 * {@code Entity#attackEntityFrom} pipeline (gated by a server config toggle), specifically so
 * {@code ignoreIFrame}/{@code allowSpecialCancel} and {@code DamageResistanceHandler}'s
 * pierce-threshold/pierce-percent values can bypass vanilla's own hardcoded armor math - a
 * necessary hack in 1.12 because {@code EntityLivingBase}'s relevant methods were private/final and
 * unreachable without reflection (see CE's {@code getDeathSoundHandle} etc. {@code MethodHandle}
 * lookups). Neo Edition's own parallel port reproduces that same bypass via a Mixin on
 * {@code LivingEntity#actuallyHurt} (see its {@code util/mixins/LivingEntityMixin.java}) - this port
 * has no Mixin infrastructure set up anywhere yet, and standing one up is a real architectural
 * decision well outside a "ballistics core" package's scope (see
 * {@code docs/phase3/gun_framework.md}'s Deferred scope - the full {@code DamageResistanceHandler}
 * armor-integration pass is its own future package). This is instead a straightforward,
 * Mixin-free translation of CE's own simpler "compatibility mode" branch
 * ({@code attackEntitySuperCompatibility}: delegate to vanilla's real damage/armor calc via
 * {@code hurt()}, then apply Sedna's own multiplier-scaled knockback on top instead of vanilla's
 * automatic knockback) - a real, already-existing CE code path, not an invented one. Its own doc
 * comment already admits the limitation this inherits: DR piercing is not applied to vanilla armor
 * values. {@code allowSpecialCancel} is accepted for call-site parity with CE but currently has no
 * effect (there is no custom cancellation gate without overriding {@code actuallyHurt} itself); a
 * future pass revisiting this alongside the full {@code DamageResistanceHandler} port should decide
 * whether standing up a Mixin is warranted for full parity.
 */
public class EntityDamageUtil {

    /**
     * "Shitty hack, if the first attack fails, it retries with damage + previous damage, allowing
     * damage to penetrate" - CE's own comment, ported verbatim. Used by the standard entity-hit/beam
     * lambdas for non-{@link LivingEntity} targets (item frames, boats, etc. - anything without
     * iframes to bypass in the first place, but which vanilla's own {@code hurt()} may otherwise
     * reject outright for unrelated reasons).
     */
    public static boolean attackEntityFromIgnoreIFrame(Entity victim, DamageSource src, float damage) {
        if (!victim.hurt(src, damage)) {
            float lastDamage = 0;
            if (victim instanceof LivingEntity living) {
                lastDamage = living.lastHurt;
            }
            return victim.hurt(src, damage + lastDamage);
        }
        return true;
    }

    /**
     * The real entry point every default {@code BulletConfig} hit-resolution lambda calls. See the
     * class javadoc for the Mixin-free simplification this makes relative to CE's full pipeline.
     */
    public static boolean attackEntityFromNT(LivingEntity living, DamageSource source, float amount, boolean ignoreIFrame, boolean allowSpecialCancel, double knockbackMultiplier, float pierceDT, float pierce) {
        if (living instanceof ServerPlayer serverPlayer && source.getEntity() instanceof Player attacker) {
            // handles the "no PVP" rule as well as scoreboard friendly fire, exactly like CE's own check
            if (!serverPlayer.canHarmPlayer(attacker)) return false;
        }

        DamageResistanceHandler.setup(pierceDT, pierce);
        try {
            if (ignoreIFrame) {
                living.invulnerableTime = 0;
            }

            Vec3 preHitMotion = living.getDeltaMovement();
            boolean hurt = living.hurt(source, amount);

            if (hurt) {
                // cancel out vanilla's own automatic knockback so Sedna's own knockbackMultiplier-scaled
                // knockback (below) is the only knockback actually applied - mirrors CE's own
                // attackEntitySuperCompatibility, which caches/restores motion around the vanilla call
                // for exactly this reason.
                living.setDeltaMovement(preHitMotion);

                Entity attacker = source.getEntity();
                if (attacker != null && knockbackMultiplier > 0) {
                    knockBack(living, attacker, knockbackMultiplier);
                }
            }

            return hurt;
        } finally {
            DamageResistanceHandler.reset();
        }
    }

    /** CE's own {@code damageArmorNT} is intentionally empty ("mlbv: yes this is empty") - ported as-is. */
    public static void damageArmorNT(LivingEntity living, float amount) {
    }

    /**
     * Direct translation of CE's {@code EntityDamageUtil.knockBack(living, attacker, damage, dX, dZ,
     * multiplier)} onto the modern {@code Vec3}-based delta-movement API - same math, expressed
     * without the mutable {@code motionX/Y/Z} fields 1.12 used.
     */
    private static void knockBack(LivingEntity living, Entity attacker, double multiplier) {
        if (living.getRandom().nextDouble() >= living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) {

            double deltaX = attacker.getX() - living.getX();
            double deltaZ = attacker.getZ() - living.getZ();
            while (deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D) {
                deltaX = (Math.random() - Math.random()) * 0.01D;
                deltaZ = (Math.random() - Math.random()) * 0.01D;
            }

            living.hasImpulse = true;
            double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double magnitude = 0.4D * multiplier;

            Vec3 motion = living.getDeltaMovement();
            double newX = motion.x / 2.0D - deltaX / horizontal * magnitude;
            double newY = motion.y / 2.0D + magnitude;
            double newZ = motion.z / 2.0D - deltaZ / horizontal * magnitude;

            if (newY > 0.2D) newY = 0.2D * multiplier;

            living.setDeltaMovement(newX, newY, newZ);
        }
    }
}
