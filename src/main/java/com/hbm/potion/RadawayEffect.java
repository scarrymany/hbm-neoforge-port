package com.hbm.potion;

import com.hbm.capability.HbmLivingProps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of CE's {@code HbmPotion.radaway} branch (upstream {@code HbmPotion.java:112-115},
 * {@code isReady:162-165}: ticks every tick). Removes {@code (amplifier+1)*0.05F} rads/tick via the
 * already-committed {@link HbmLivingProps#incrementRadiation} - verified clamp-compatible with CE's
 * real {@code EntityHbmProps#decreaseRads} (both clamp the result to a {@code [0, MAX]} range, this
 * port's {@code MAX_RADS} replacing CE's hardcoded {@code 2500} literal).
 * <p>
 * <b>Formula, verified against CE - do not copy Neo Edition's {@code RadawayEffect}</b>, which
 * removes a flat {@code -(amplifier+1)} rads/tick with no {@code *0.05F} factor, 20x stronger than
 * CE's real balance (and asymmetric with {@link RadiationEffect}'s own {@code *0.05F} grant, which
 * the two are meant to mirror).
 */
public class RadawayEffect extends MobEffect {

    public RadawayEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        HbmLivingProps.incrementRadiation(entity, -(amplifier + 1) * 0.05F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
