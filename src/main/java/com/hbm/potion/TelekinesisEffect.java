package com.hbm.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of CE's {@code HbmPotion.telekinesis} branch (upstream {@code HbmPotion.java:132-141},
 * {@code isReady:162-165}: ticks every tick). While more than 1 tick of duration remains, adds a
 * small random {@code (rand-0.5)*(amplifier+1)*0.5} impulse to all 3 motion axes.
 * <p>
 * CE's own {@code isReady} always returns {@code true} for this effect (so
 * {@link #shouldApplyEffectTickThisTick} does too here, matching CE exactly); the
 * "more than 1 tick remains" gate is a <i>separate</i>, inner check CE performs inside
 * {@code performEffect} itself by re-reading the live {@code PotionEffect}'s remaining duration -
 * reproduced here the same way, via {@link LivingEntity#getEffect} against this effect's own
 * {@link HbmPotionEffects#TELEKINESIS} holder (a lazy, tick-time lookup - safe against this port's
 * documented "static field initializer touching a DeferredHolder before RegisterEvent fires" crash
 * pattern, since this only runs once the entity already has the effect applied).
 * <p>
 * <b>Zero real CE call sites exist anywhere in CE's own source tree</b> for this effect (confirmed
 * by repo-wide grep) - dead/vestigial content, not a missing consumer in this port. Ported anyway
 * for roster completeness, matching CE's own {@code registerPotions} registering all 12
 * unconditionally and this port's general practice of preserving full CE inventories even for
 * content with no current consumer.
 */
public class TelekinesisEffect extends MobEffect {

    public TelekinesisEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        MobEffectInstance instance = entity.getEffect(HbmPotionEffects.TELEKINESIS);
        if (instance == null || instance.getDuration() <= 1) return true;

        double dx = (entity.getRandom().nextFloat() - 0.5) * (amplifier + 1) * 0.5;
        double dy = (entity.getRandom().nextFloat() - 0.5) * (amplifier + 1) * 0.5;
        double dz = (entity.getRandom().nextFloat() - 0.5) * (amplifier + 1) * 0.5;

        entity.setDeltaMovement(entity.getDeltaMovement().add(dx, dy, dz));
        entity.hasImpulse = true;

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
