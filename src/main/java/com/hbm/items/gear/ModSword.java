package com.hbm.items.gear;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ModSword} (generic sword base). CE special-cased tooltip flavor text for six
 * named swords via {@code this == ModItems.X} identity checks; post-flattening every registry name
 * is already its own distinct {@link ModSword} instance, so the identity check becomes a registry
 * path comparison instead - {@link #appendHoverText} is the only override, matching CE exactly.
 */
public class ModSword extends SwordItem {

    public ModSword(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "weapon_saw" -> tooltip.add(Component.literal("Prepare for your examination!"));
            case "weapon_bat" -> tooltip.add(Component.literal("Do you like hurting other people?"));
            case "weapon_bat_nail" -> tooltip.add(Component.literal("Or is it a classic?"));
            case "weapon_golf_club" -> tooltip.add(Component.literal("Property of Miami Beach Golf Club."));
            case "weapon_pipe_rusty" -> tooltip.add(Component.literal("Ouch! Ouch! Ouch!"));
            case "weapon_pipe_lead" -> tooltip.add(Component.literal("Manually override anything by smashing it with this pipe."));
            case "reer_graar" -> {
                tooltip.add(Component.literal("Call now!"));
                tooltip.add(Component.literal("555-10-3728-ZX7-INFINITE"));
            }
            default -> {
            }
        }
    }
}
