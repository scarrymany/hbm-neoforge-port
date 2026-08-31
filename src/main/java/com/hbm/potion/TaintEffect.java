package com.hbm.potion;

import com.hbm.damage.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code HbmPotion.taint} branch (upstream {@code HbmPotion.java:93-108},
 * {@code isReady:158-161}). Every-other-tick ({@code duration % 2 == 0}) 1-in-80 chance to deal
 * {@code amplifier+1} {@link ModDamageTypes#TAINT} damage.
 * <p>
 * <b>{@code EntityCreeperTainted} self-damage exemption</b> - CE skips the self-damage roll for that
 * one mob specifically; now wired below (Phase 4, {@code docs/phase4/entities_creeper_variants.md} -
 * an independent immunity axis from {@code util.ContaminationUtil#isRadImmune}'s own,
 * interface-based {@code IRadiationImmune} check, since CE itself keys this exemption directly off
 * the class rather than that mob's rad-immunity).
 * <p>
 * <b>Not reproduced (1 remaining forward reference):</b> CE additionally spreads a {@code BlockTaint}
 * taint block onto the solid ground below when {@code ServerConfig.TAINT_TRAILS} is on <b>and</b>
 * {@link HbmPotionEffects#isWarDim}. Unlike the other 2 {@code isWarDim}-gated branches in this
 * package ({@link BangEffect}, {@link PhosphorusEffect}), this one has a <i>second</i>, independent
 * blocker: neither {@code ModBlocks.taint} nor a {@code BlockTaint} class exists anywhere in this
 * port yet (confirmed by repo-wide grep - a strictly wider gap than the {@code isWarDim} stub this
 * area's research report names). Dropped entirely until whichever package ports that block restores
 * this sub-feature.
 */
public class TaintEffect extends MobEffect {

    public TaintEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (!(entity instanceof com.hbm.entity.mob.EntityCreeperTainted) && level.getRandom().nextInt(80) == 0) {
            DamageSource src = entity.damageSources().source(ModDamageTypes.TAINT);
            entity.hurt(src, amplifier + 1);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }
}
