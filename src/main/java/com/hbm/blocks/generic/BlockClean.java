package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.config.RadiationConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * "Clean zone" marker block, ported from CE's {@code BlockClean}. Reads
 * {@link RadiationConfig#ENABLE_CONTAMINATION_ON_GROUND} only, no radiation-system call.
 */
public class BlockClean extends BlockBase {

    public BlockClean(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!RadiationConfig.ENABLE_CONTAMINATION_ON_GROUND.get()) {
            return;
        }
        tooltip.add(Component.translatable("trait.cleanroom").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("trait.cleanroom.desc").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
