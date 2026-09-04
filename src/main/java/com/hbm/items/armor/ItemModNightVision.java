package com.hbm.items.armor;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Exact CE {@code ItemModNightVision} ({@code ItemModNightVision.java:38-57}).
 * HUD-on: NV 15s + flag on the host armor. HUD-off: strip NV only if this mod applied it.
 */
public class ItemModNightVision extends ItemArmorMod {

    public ItemModNightVision(Properties properties) {
        super(properties, ArmorModHandler.helmet_only, true, false, false, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal(I18nUtil.resolveKey("item.night_vision.description.item")).withStyle(ChatFormatting.AQUA));
        components.add(Component.empty());
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(Component.literal(I18nUtil.resolveKey("item.night_vision.description.in_armor", stack.getHoverName().getString()))
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide() && entity instanceof Player player
                && armor.getItem() instanceof ArmorFSBPowered && ArmorFSB.hasFSBArmor(player)) {
            if (HbmPlayerAttachment.getData(player).getEnableHUD()) {
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20));
                armor.set(ArmorDataComponents.NIGHT_VISION_ACTIVE.get(), true);

                if (entity.getRandom().nextInt(200) == 0) {
                    armor.hurtAndBreak(1, entity, EquipmentSlot.HEAD);
                }
            } else if (Boolean.TRUE.equals(armor.get(ArmorDataComponents.NIGHT_VISION_ACTIVE.get()))) {
                entity.removeEffect(MobEffects.NIGHT_VISION);
                armor.remove(ArmorDataComponents.NIGHT_VISION_ACTIVE.get());
            }
        }
    }
}
