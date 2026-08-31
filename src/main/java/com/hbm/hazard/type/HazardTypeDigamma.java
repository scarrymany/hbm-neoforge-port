package com.hbm.hazard.type;

import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ContaminationUtil;
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

public class HazardTypeDigamma implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        ContaminationUtil.applyDigammaData(target, (float) ((level * stack.getCount() / 20D) * IHazardType.hazardRate()));
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);

        final double displayLevel = Math.round(level * 10000D) / 10D;
        list.add(Component.literal("[" + I18nUtil.resolveKey("trait.digamma") + "]").withStyle(ChatFormatting.RED));
        list.add(Component.literal(displayLevel + "mDRX/s").withStyle(ChatFormatting.DARK_RED));

        if (stack.getCount() > 1) {
            final double stackLevel = displayLevel * stack.getCount();
            list.add(Component.literal("Stack: " + stackLevel + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
