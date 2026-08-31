package com.hbm.hazard.type;

import com.hbm.config.GeneralConfig;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.lib.Library;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
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

public class HazardTypeRadiation implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, double level, final ItemStack stack) {

        final boolean reacher = HazardHelper.isHoldingReacher(target);

        level *= stack.getCount();

        if (level > 0) {
            double rad = level / 20D;

            if (GeneralConfig.enable528() && reacher) {
                rad = rad / 49D; // More realistic function for 528: x / distance^2
            } else if (reacher) {
                rad = Math.sqrt(rad); // Reworked radiation function: sqrt(x+1/(x+2)^2)-1/(x+2)
            }

            ContaminationUtil.contaminate(target, HazardType.RADIATION, ContaminationType.CREATIVE, (float) (rad * IHazardType.hazardRate()));
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, double level, final ItemStack stack, final List<IHazardModifier> modifiers) {

        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);
        if (level == 0) return;

        list.add(Component.literal("[" + I18nUtil.resolveKey("trait.radioactive") + "]").withStyle(ChatFormatting.GREEN));
        list.add(Component.literal(" " + Library.roundFloat((float) getNewValue(level), 3) + getSuffix(level) + " " + I18nUtil.resolveKey("desc.rads")).withStyle(ChatFormatting.YELLOW));

        if (stack.getCount() > 1) {
            final double stackRad = level * stack.getCount();
            list.add(Component.literal(" " + I18nUtil.resolveKey("desc.stack") + " " + Library.roundFloat((float) getNewValue(stackRad), 3) + getSuffix(stackRad) + " " + I18nUtil.resolveKey("desc.rads")).withStyle(ChatFormatting.YELLOW));
        }
    }

    public static double getNewValue(final double radiation) {
        if (radiation < 1000000) {
            return radiation;
        } else if (radiation < 1000000000) {
            return radiation * 0.000001D;
        } else {
            return radiation * 0.000000001D;
        }
    }

    public static String getSuffix(final double radiation) {
        if (radiation < 1000000) {
            return "";
        } else if (radiation < 1000000000) {
            return I18nUtil.resolveKey("desc.mil");
        } else {
            return I18nUtil.resolveKey("desc.bil");
        }
    }
}
