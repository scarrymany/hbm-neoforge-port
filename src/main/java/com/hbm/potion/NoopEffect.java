package com.hbm.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared no-op {@link MobEffect} for CE's 4 marker-only {@code HbmPotion} fields ({@code mutation},
 * {@code radx}, {@code stability}, {@code death}). None of these 4 have a branch in CE's real
 * {@code performEffect}/{@code isReady} dispatch chain - {@code isReady} falls through to
 * {@code return false} for all 4, so {@code performEffect} is never invoked for them either. They
 * exist purely as boolean flags other systems read via {@code entity.isPotionActive(HbmPotion.X)}
 * (this port: {@code LivingEntity#hasEffect(Holder)}):
 * <ul>
 *     <li>{@code mutation} - full radiation-damage immunity ({@code util.ContaminationUtil
 *     #calculateRadiationMod}/{@code #isRadImmune}) and a hazmat-suit-equivalent fallback
 *     ({@code handler.ArmorUtil#checkForHazmat})</li>
 *     <li>{@code radx} - a flat {@code +0.2F} radiation-resistance bonus
 *     ({@code handler.HazmatRegistry#getResistance(LivingEntity)})</li>
 *     <li>{@code stability} - digamma-gain immunity ({@code util.ContaminationUtil
 *     #applyDigammaData}) and a fau/dns-armor-equivalent fallback
 *     ({@code handler.ArmorUtil#checkForDigamma})</li>
 *     <li>{@code death} - a purely cosmetic joke effect (CE itself flags it {@code isBad=false},
 *     i.e. {@link MobEffectCategory#BENEFICIAL} here, not {@code HARMFUL} as Neo Edition wrongly
 *     maps it); its only real gameplay consumer is a client-side player-model swap
 *     ({@code PermaSyncHandler}/{@code MixinRenderPlayerManly}) that is Phase 5 ("Client & UX")
 *     scope, not needed for this effect to exist and be checkable</li>
 * </ul>
 */
public class NoopEffect extends MobEffect {

    public NoopEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return false;
    }
}
