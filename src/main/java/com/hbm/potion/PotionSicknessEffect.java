package com.hbm.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code HbmPotion.potionsickness} branch (upstream {@code HbmPotion.java:147-152},
 * {@code isReady:158-161}). Every other tick ({@code duration % 2 == 0}): 1-in-128 chance to grant
 * vanilla Nausea ({@link MobEffects#CONFUSION} - this port's confirmed real 1.21.1 field name for
 * CE's {@code MobEffects.NAUSEA}, matching every other already-committed call site in this port,
 * e.g. {@code items.food.ItemPill}/{@code ItemCanteen}/{@code ItemEnergy}) for 8 seconds
 * (amplifier 0).
 * <p>
 * Also the payload of {@code config.VersatileConfig#applyPotionSickness}/{@code #hasPotionSickness}
 * (restored by this same package - see that class).
 * <p>
 * CE flags this effect {@code isBad=false} (i.e. {@link MobEffectCategory#BENEFICIAL} here) - do
 * not copy Neo Edition's invented {@code NEUTRAL} mapping, which has no CE equivalent (CE's own
 * {@code Potion} model is strictly binary).
 */
public class PotionSicknessEffect extends MobEffect {

    public PotionSicknessEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (level.getRandom().nextInt(128) == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 8 * 20, 0));
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }
}
