package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class HazardTypeBlinding implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.DISABLE_BLINDING.get()) return;

        if (!ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, HazardClass.LIGHT)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) level * IHazardType.hazardRate(), 0));
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[" + I18nUtil.resolveKey("trait.blinding") + "]").withStyle(ChatFormatting.DARK_AQUA));
    }
}
