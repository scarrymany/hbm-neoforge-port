package com.hbm.potion;

import com.hbm.damage.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of CE's {@code HbmPotion.lead} branch (upstream {@code HbmPotion.java:128-131},
 * {@code isReady:170-174}). Once every 60 ticks ({@code duration % 60 == 0}, no dimension gate):
 * deal {@code amplifier+1} {@link ModDamageTypes#LEAD} damage.
 * <p>
 * <b>Cadence, verified against CE - do not copy Neo Edition's {@code LeadEffect}</b>, which
 * unconditionally returns {@code true} from {@code shouldApplyEffectTickThisTick} and so ticks
 * every game tick, 60x more frequently than CE's real balance.
 */
public class LeadEffect extends MobEffect {

    public LeadEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        DamageSource src = entity.damageSources().source(ModDamageTypes.LEAD);
        entity.hurt(src, amplifier + 1);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }
}
