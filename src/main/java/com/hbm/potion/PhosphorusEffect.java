package com.hbm.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code HbmPotion.phosphorus} branch (upstream {@code HbmPotion.java:142-145},
 * {@code isReady:162-165}: ticks every tick). While {@link HbmPotionEffects#isWarDim}: sets the
 * entity ablaze for {@code amplifier+1} seconds (CE: {@code entity.setFire(level+1)}; this port's
 * confirmed-real modern equivalent, {@code LivingEntity#igniteForSeconds(int)}, already used
 * identically by {@code handler.ability.IWeaponAbility#FIRE}).
 * <p>
 * Also the payload of {@code handler.ability.IWeaponAbility#PHOSPHORUS} (60/90-tick-by-level
 * duration, amplifier 4), applied by melee weapons carrying that ability (e.g. {@code mese_gavel}).
 * <p>
 * <b>Do not copy Neo Edition's {@code PhosphorusEffect}</b>, which sets a hardcoded 1-second fire
 * timer every tick unconditionally, with no {@code isWarDim} gate and no use of the amplifier at
 * all.
 */
public class PhosphorusEffect extends MobEffect {

    public PhosphorusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (HbmPotionEffects.isWarDim(level)) {
            entity.igniteForSeconds(amplifier + 1);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
