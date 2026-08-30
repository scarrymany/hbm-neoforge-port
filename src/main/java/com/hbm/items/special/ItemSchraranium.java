package com.hbm.items.special;

import com.hbm.config.GeneralConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemSchraranium} (com.hbm.items.special.ItemSchraranium), backing
 * {@code ingot_schraranium}: a cosmetic LBSM-compat override that renames the item to "Nikonium"
 * and adds a joke tooltip line when the LBSM addon's "full schrabidium" option is enabled.
 * <p>
 * CE also swapped the item's rendered model via {@code addPropertyOverride}, a 1.12 item-model
 * predicate mechanism with no equivalent needed here - model/texture swapping is a resource-pack
 * concern for a later asset-authoring pass, not Java registration code.
 */
public class ItemSchraranium extends Item {

    public ItemSchraranium(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (GeneralConfig.enableLBSM() && GeneralConfig.LBSM_FULL_SCHRAB.get()) {
            return Component.translatable("item.hbm.ingot_nikonium");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (GeneralConfig.enableLBSM() && GeneralConfig.LBSM_FULL_SCHRAB.get()) {
            tooltip.add(Component.literal("pankæk"));
        }
    }
}
