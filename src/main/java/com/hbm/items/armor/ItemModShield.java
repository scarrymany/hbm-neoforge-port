package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ItemModShield} - a {@code kevlar}-slot
 * {@link ItemArmorMod} insert that raises the wearer's effective max shield capacity beyond
 * {@code ItemFlask}'s base value. Already forward-referenced by name from this port's own
 * already-ported {@code com.hbm.capability.HbmPlayerAttachment#getEffectiveMaxShield(Player)}
 * (see {@code docs/phase3/fsb_armor_and_jetpacks.md} supporting-classes table) - that reference is
 * what this class exists to satisfy; the shield mechanic itself is a completely separate,
 * already-ported system (see {@link com.hbm.items.gear.ArmorFSB}'s class javadoc for the same
 * "FSB is not a shield" clarification).
 */
public class ItemModShield extends ItemArmorMod {

    public final float shield;

    public ItemModShield(Properties properties, float shield) {
        super(properties, ArmorModHandler.kevlar, false, true, false, false);
        this.shield = shield;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        ChatFormatting color = System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
        components.add(Component.literal("+" + (Math.round(shield * 10) * 0.1) + " shield").withStyle(color));
        components.add(Component.empty());
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        ChatFormatting color = System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
        list.add(Component.literal("  ").append(stack.getHoverName()).append(" (+" + (Math.round(shield * 10) * 0.1) + " health)").withStyle(color));
    }
}
