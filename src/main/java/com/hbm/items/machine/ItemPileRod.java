package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

/**
 * Tooltip-only base item for the classic "pile" reactor - no tile entity reference. CE's per-instance
 * subclassing (one field per rod material) becomes one plain registered instance per material here.
 */
public class ItemPileRod extends ItemBase {

    public ItemPileRod(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Arrays.stream(I18nUtil.resolveKeyArray("desc.item.pileRod")).map(Component::literal).forEach(tooltip::add);

        String descKey = this.getDescriptionId() + ".desc";
        if (I18nUtil.exist(descKey)) {
            Arrays.stream(I18nUtil.resolveKeyArray(descKey)).map(Component::literal).forEach(tooltip::add);
        }
    }
}
