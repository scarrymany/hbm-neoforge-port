package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

import static com.hbm.hazard.helper.HazardHelper.applyPotionEffect;

public class HazardTypeToxic implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.DISABLE_TOXIC.get()) return;

        final boolean reacher = HazardHelper.isHoldingReacher(target);
        boolean hasToxFilter = false;
        boolean hasHazmat = false;

        if (target instanceof Player player) {
            hasToxFilter = ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, ArmorRegistry.HazardClass.NERVE_AGENT);

            if (hasToxFilter) {
                ArmorUtil.damageGasMaskFilter(player, IHazardType.hazardRate());
            }

            hasHazmat = ArmorUtil.checkForHazmat(player);
        }

        final boolean isUnprotected = !(hasToxFilter || hasHazmat || reacher);

        if (isUnprotected) {
            applyPotionEffect(target, MobEffects.WEAKNESS, 110, (int) (level - 1));

            if (level > 2) {
                applyPotionEffect(target, MobEffects.MOVEMENT_SLOWDOWN, 110, (int) Math.min(4, level - 4));
            }

            if (level > 4) {
                applyPotionEffect(target, MobEffects.HUNGER, 110, (int) level);
            }

            if (level > 6 && target.level().random.nextInt((int) (2000 / level)) == 0) {
                applyPotionEffect(target, MobEffects.POISON, 110, (int) (level - 4));
            }
        }

        if (!hasHazmat || !hasToxFilter || !reacher) {
            if (level > 8) {
                applyPotionEffect(target, MobEffects.DIG_SLOWDOWN, 110, (int) (level - 8));
            }

            if (level > 16) {
                applyPotionEffect(target, MobEffects.HARM, 110, (int) (level - 16));
            }
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        final String adjectiveKey;

        if (level > 16) {
            adjectiveKey = "adjective.extreme";
        } else if (level > 8) {
            adjectiveKey = "adjective.veryhigh";
        } else if (level > 4) {
            adjectiveKey = "adjective.high";
        } else if (level > 2) {
            adjectiveKey = "adjective.medium";
        } else {
            adjectiveKey = "adjective.little";
        }

        list.add(Component.literal("[" + I18nUtil.resolveKey(adjectiveKey) + " " + I18nUtil.resolveKey("trait.toxic") + "]").withStyle(ChatFormatting.GREEN));
    }
}
