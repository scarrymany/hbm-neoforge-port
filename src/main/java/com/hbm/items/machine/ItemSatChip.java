package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

/**
 * Satellite payload chip. Tooltip-only descriptor item; CE identity-checked {@code this ==
 * ModItems.sat_foeq} (etc.) to append a per-instance description key, which becomes a constructor
 * parameter here instead.
 */
public class ItemSatChip extends ItemBase implements ISatChip {

    private final String descKey;

    public ItemSatChip(String descKey, Properties properties) {
        super(properties);
        this.descKey = descKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.satellitefr", getFreq(stack))));
        if (this.descKey != null) {
            if (I18nUtil.exist(this.descKey + ".desc")) {
                Arrays.stream(I18nUtil.resolveKeyArray(this.descKey + ".desc")).map(Component::literal).forEach(tooltip::add);
            } else {
                tooltip.add(Component.literal(I18nUtil.resolveKey(this.descKey)));
            }
        }
    }
}
