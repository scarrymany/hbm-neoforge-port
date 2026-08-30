package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

import static com.hbm.hazard.helper.HazardHelper.applyPotionEffect;

public class HazardTypeCold implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        final boolean reacher = HazardHelper.isHoldingReacher(target);
        if (RadiationConfig.DISABLE_COLD.get() || reacher) return;

        if (target instanceof Player && ArmorUtil.checkForHazmat(target)) return;

        final int baseLevel = (int) level - 1;
        final int witherLevel = (int) level - 3;

        applyPotionEffect(target, MobEffects.DIG_SLOWDOWN, 110, baseLevel);
        applyPotionEffect(target, MobEffects.MOVEMENT_SLOWDOWN, 110, Math.min(4, baseLevel));
        applyPotionEffect(target, MobEffects.WEAKNESS, 110, baseLevel);

        if (level > 4) {
            applyPotionEffect(target, MobEffects.WITHER, 110, witherLevel);
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[" + I18nUtil.resolveKey("trait.cryogenic") + "]").withStyle(ChatFormatting.AQUA));
    }
}
