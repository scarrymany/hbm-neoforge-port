package com.hbm.hazard.helper;

import com.hbm.config.GeneralConfig;
import com.hbm.items.tool.ToolItems;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class HazardHelper {

    public static boolean isHoldingReacher(final LivingEntity target) {
        if (target instanceof Player player && !GeneralConfig.enable528()) {
            return player.getMainHandItem().is(ToolItems.REACHER.get()) || player.getOffhandItem().is(ToolItems.REACHER.get());
        }
        return false;
    }

    public static void applyPotionEffect(final LivingEntity target, final Holder<MobEffect> effect, final int duration, final int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }
}
