package com.hbm.config;

/**
 * Port of CE's {@code VersatileConfig}: derived-logic helpers that combine values from other
 * config classes rather than holding config values of their own.
 * <p>
 * Not ported: {@code applyPotionSickness(EntityLivingBase, int)} and
 * {@code hasPotionSickness(EntityLivingBase)}. Both depend on {@code com.hbm.potion.HbmPotion},
 * which is out of this area's scope and not ported yet. Whoever ports the potion system should
 * add these back, reading {@link PotionConfig#potionSicknessMode()} for the mode check CE did via
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
