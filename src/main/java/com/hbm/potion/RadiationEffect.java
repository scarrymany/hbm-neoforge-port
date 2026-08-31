package com.hbm.potion;

import com.hbm.util.ContaminationUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of CE's {@code HbmPotion.radiation} branch (upstream {@code HbmPotion.java:109-111},
 * {@code isReady:162-165}: ticks every tick). One line: creative-tier contamination of
 * {@code (amplifier+1)*0.05F} rad/tick via the already-ported {@link ContaminationUtil#contaminate}.
 */
public class RadiationEffect extends MobEffect {

    public RadiationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION,
                ContaminationUtil.ContaminationType.CREATIVE, (amplifier + 1F) * 0.05F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
