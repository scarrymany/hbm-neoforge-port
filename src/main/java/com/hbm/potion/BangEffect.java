package com.hbm.potion;

import com.hbm.damage.ModDamageTypes;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code HbmPotion.bang} branch (upstream {@code HbmPotion.java:116-127},
 * {@code isReady:166-169}: only active in the last <= 10 ticks of the effect's remaining
 * duration - a delayed-fuse "you have a few seconds' warning" mechanic).
 * <p>
 * Lethal payload (guarded by {@link HbmPotionEffects#isWarDim}): {@code 10000*(amplifier+1)}
 * {@link ModDamageTypes#BANG} damage through the normal, invulnerability/creative-respecting
 * {@code hurt} path for every entity, plus an <b>additional</b>, unconditional force-kill for
 * non-players only (CE: {@code onDeath}+{@code setHealth(0)}; this port's confirmed-real modern
 * equivalent, {@code setHealth(0F)} then {@code die(source)}, already used identically by
 * {@code handler.ability.IWeaponAbility#VAMPIRE}). Sound + a 10-particle burst play
 * <b>unconditionally</b>, regardless of dimension, matching CE exactly.
 * <p>
 * <b>Do not use {@code Entity#kill()} here</b> (Neo Edition's bug, called out explicitly in this
 * area's research report): it bypasses invulnerability/creative checks entirely and would even
 * instant-kill a creative-mode player if applied unconditionally to every entity - CE never does
 * that. The normal {@code hurt()} call above already respects those checks for everyone; only the
 * <i>additional</i> non-player force-kill bypasses them, exactly as CE's own {@code onDeath} call
 * does.
 */
public class BangEffect extends MobEffect {

    public BangEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (HbmPotionEffects.isWarDim(level)) {
            DamageSource src = entity.damageSources().source(ModDamageTypes.BANG);
            entity.hurt(src, 10000F * (amplifier + 1));

            if (!(entity instanceof Player)) {
                entity.setHealth(0F);
                entity.die(src);
            }
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                HBMSoundHandler.laserBang.get(), SoundSource.AMBIENT, 100.0F, 1.0F);
        ExplosionLarge.spawnParticles(level, entity.getX(), entity.getY(), entity.getZ(), 10);

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration <= 10;
    }
}
