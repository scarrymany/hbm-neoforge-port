package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * CE {@code ItemModCladding} — armor-mod cladding with a flat rad-resist bonus.
 * {@code ModItems.java:191-197}.
 */
public class ItemModCladding extends ItemArmorMod {

    public final double rad;

    public ItemModCladding(Properties properties, double rad) {
        super(properties, ArmorModHandler.cladding, true, true, true, true);
        this.rad = rad;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("+" + rad + " rad-resistance").withStyle(ChatFormatting.YELLOW));
        components.add(Component.empty());
        super.appendHoverText(stack, context, components, flag);
    }
}
