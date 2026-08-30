package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.util.List;

/**
 * Rotatable decorative pillar, ported from CE's {@code BlockRotatablePillar}. CE's two constructor
 * overloads (with/without an explicit {@link net.minecraft.world.level.block.SoundType}) collapse
 * to one here, since sound type is now part of the {@code Properties} built at the registration
 * call site rather than a post-construction setter. The two hardcoded schrabidium/euphemium cluster
 * tooltip lines are preserved via direct sibling-field references, matching CE's own
 * {@code ModBlocks.block_schrabidium_cluster} identity check.
 */
public class BlockRotatablePillar extends RotatedPillarBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockRotatablePillar(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (this == ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get()) {
            tooltip.add(Component.translatable("tile.block_schrabidium_cluster.desc"));
        }
        if (this == ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()) {
            tooltip.add(Component.translatable("tile.block_euphemium_cluster.desc"));
        }

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
