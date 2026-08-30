package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Damage-tracked laser lens for a particle/laser machine that doesn't exist yet - tooltip-only
 * descriptor item, no tile entity reference in CE.
 */
public class ItemLens extends ItemBase {

    private final long maxDamage;

    public ItemLens(long maxDamage, Properties properties) {
        super(properties);
        this.maxDamage = maxDamage;
    }

    public static long getLensDamage(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.LENS_DAMAGE.get(), 0L);
    }

    public static void setLensDamage(ItemStack stack, long damage) {
        stack.set(MachineDataComponents.LENS_DAMAGE.get(), damage);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLensDamage(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getLensDamage(stack) / (float) this.maxDamage);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40C0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long damage = getLensDamage(stack);
        double percent = (int) ((this.maxDamage - damage) * 100000000D / this.maxDamage) / 1000000D;

        tooltip.add(Component.literal(ChatFormatting.DARK_AQUA + I18nUtil.resolveKey("desc.durticks") + " " + (this.maxDamage - damage) + " / " + this.maxDamage));
        tooltip.add(Component.literal(ChatFormatting.DARK_AQUA + I18nUtil.resolveKey("desc.durpercents") + " " + percent + "%"));
    }
}
