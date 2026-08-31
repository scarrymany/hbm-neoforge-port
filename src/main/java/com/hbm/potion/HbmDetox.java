package com.hbm.potion;

import com.hbm.config.PotionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * Port of CE's {@code com.hbm.potion.HbmDetox}: a bare "is this effect on the hazmat-bacteria-
 * protection blacklist" check, backed by {@link PotionConfig#potionBlacklist()} (already ported;
 * default {@code ["srparasites:coth", "srparasites:viral"]} - a <i>modded</i>-disease-compat
 * blacklist, not a vanilla-potion one).
 * <p>
 * CE's {@code blacklistedPotions} field eagerly resolved each blacklist string into a real
 * {@code Potion} object once at {@code preinit()} time and cached the resulting object set. This
 * port instead re-reads {@link PotionConfig#potionBlacklist()} (itself a plain {@code Set<String>}
 * snapshot of a config value with no registry lookups of its own) on every call rather than caching
 * a resolved set at class-load time - avoiding any dependency on mod-config load having completed
 * before this class is first touched, at the cost of a handful of string comparisons per call.
 * CE's only real call site, {@code ModEventHandler.potionCheck}, runs at most once per
 * potion-effect application attempt, not a hot loop, so this trade is free in practice.
 * <p>
 * CE's only real consumer, {@code ModEventHandler.potionCheck} (a {@code PotionApplicableEvent}
 * listener vetoing blacklisted-potion application onto an entity wearing a full hazmat set with
 * bacteria head protection, damaging the gas-mask filter as a side effect), is <b>not</b> wired
 * here - see docs/phase4/hbm_potion_system.md's Open questions section: the exact NeoForge 1.21.1
 * event name/shape for CE's {@code PotionApplicableEvent} is not confirmed against a real compiled
 * jar or either reference repo in this sandbox. This class is fully usable once that listener is
 * written; only the listener itself is deferred.
 */
public final class HbmDetox {

    private HbmDetox() {
    }

    public static boolean isBlacklisted(MobEffect effect) {
        if (effect == null) return false;

        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id != null && PotionConfig.potionBlacklist().contains(id.toString());
    }
}
