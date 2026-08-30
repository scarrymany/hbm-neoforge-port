package com.hbm.blocks.generic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.util.List;

/**
 * Decorative jungle-ruin log, ported from CE's {@code BlockPinkLog}. CE extended 1.12's
 * {@code BlockLog} purely for its axis-aligned {@code LOG_AXIS} property and per-axis texture
 * rotation - vanilla's modern {@link RotatedPillarBlock} (the base every vanilla log already
 * extends) provides exactly the same {@code AXIS} property and placement/rotation behavior
 * natively, so this collapses to a thin subclass carrying only CE's extra tooltip line, the same
 * shape already used for {@link BlockRotatablePillar}/{@link BlockRadResistantPillar}.
 */
public class BlockPinkLog extends RotatedPillarBlock {

    public BlockPinkLog(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
