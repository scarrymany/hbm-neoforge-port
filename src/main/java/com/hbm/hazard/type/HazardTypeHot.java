package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class HazardTypeHot implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        final boolean wetOrReacher = HazardHelper.isHoldingReacher(target) || target.isInWaterOrRain();
        if (RadiationConfig.DISABLE_HOT.get() || wetOrReacher) return;
        if (target instanceof Player player && player.isCreative()) return;
        target.setRemainingFireTicks((int) Math.ceil(level) * hazardRate);
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, double level, final ItemStack stack, final List<IHazardModifier> modifiers) {

        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);

        if (level > 0) {
            list.add(Component.literal("[" + I18nUtil.resolveKey("trait.hot") + "]").withStyle(ChatFormatting.GOLD));
        }
    }
}
