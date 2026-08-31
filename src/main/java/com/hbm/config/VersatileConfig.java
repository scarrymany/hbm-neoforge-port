package com.hbm.config;

import com.hbm.potion.HbmPotionEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of CE's {@code VersatileConfig}: derived-logic helpers that combine values from other
 * config classes rather than holding config values of their own.
 * <p>
 * {@code applyPotionSickness(EntityLivingBase, int)}/{@code hasPotionSickness(EntityLivingBase)}
 * are restored here against {@code com.hbm.potion.HbmPotionEffects#POTIONSICKNESS}, reading
 * {@link PotionConfig#potionSicknessMode()} for the mode check CE did via
 * {@code PotionConfig.potionSickness}.
 */
public class VersatileConfig {

    private static final int MINUTE_TICKS = 60 * 20;
    private static final int HOUR_TICKS = 60 * MINUTE_TICKS;

    private VersatileConfig() {}

    /** Mirrors CE's {@code VersatileConfig.getSchrabOreChance()}. */
    public static int getSchrabOreChance() {
        return GeneralConfig.enableLBSM() ? 20 : 250;
    }

    /**
     * Mirrors CE's {@code VersatileConfig.applyPotionSickness(EntityLivingBase, int)}: a no-op when
     * {@link PotionConfig.SicknessMode#OFF}, a {@code x12} duration multiplier under
     * {@link PotionConfig.SicknessMode#TERRARIA}, otherwise grants
     * {@code com.hbm.potion.HbmPotionEffects#POTIONSICKNESS} for {@code duration*20} ticks at
     * amplifier 0.
     */
    public static void applyPotionSickness(LivingEntity entity, int duration) {
        PotionConfig.SicknessMode mode = PotionConfig.potionSicknessMode();
        if (mode == PotionConfig.SicknessMode.OFF) return;

        if (mode == PotionConfig.SicknessMode.TERRARIA) duration *= 12;

        entity.addEffect(new MobEffectInstance(HbmPotionEffects.POTIONSICKNESS, duration * 20, 0));
    }

    /** Mirrors CE's {@code VersatileConfig.hasPotionSickness(EntityLivingBase)}. */
    public static boolean hasPotionSickness(LivingEntity entity) {
        return entity.hasEffect(HbmPotionEffects.POTIONSICKNESS);
    }

    /** Mirrors CE's {@code VersatileConfig.rtgDecay()}. */
    public static boolean rtgDecay() {
        return GeneralConfig.enable528() || MachineConfig.DO_RTGS_DECAY.get();
    }

    /** Mirrors CE's {@code VersatileConfig.getLongDecayChance()} (identical to the short variant in CE). */
    public static int getLongDecayChance() {
        return decayChance();
    }

    /** Mirrors CE's {@code VersatileConfig.getShortDecayChance()} (identical to the long variant in CE). */
    public static int getShortDecayChance() {
        return decayChance();
    }

    private static int decayChance() {
        if (GeneralConfig.enable528()) return 15 * HOUR_TICKS;
        if (GeneralConfig.enableLBSM() && GeneralConfig.LBSM_SHORT_DECAY.get()) return 15 * MINUTE_TICKS;
        return 3 * HOUR_TICKS;
    }
}
