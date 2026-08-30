package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

/**
 * Plain durability-tracked blade item (foundry mold output: {@code blade_titanium}/
 * {@code blade_tungsten}, and the {@code blades_*} multi-blade variants). CE based this on
 * {@code ItemCustomLore}, a rarity/lore base class from {@code items.special} that is not ported
 * here (out of this area's scope, and its only load-bearing behavior for this item was the
 * generic ".desc" tooltip lookup, reproduced directly below).
 */
public class ItemBlades extends ItemBase {

    public ItemBlades(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = this.getDescriptionId() + ".desc";
        if (I18nUtil.exist(key)) {
            tooltip.addAll(Arrays.stream(I18nUtil.resolveKeyArray(key)).map(Component::literal).toList());
        }
    }
}
