package com.hbm.blocks.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SlabBlock;

import java.util.List;

/**
 * Generic slab base for a mod building material, replacing CE's {@code BlockGenericSlab}.
 * <p>
 * CE needed two cooperating block classes (a "single" slab and a hand-maintained "double" slab
 * that dropped/picked the single variant) because 1.12's {@code BlockSlab} had no notion of a
 * slab owning its own doubled state. Vanilla's modern {@link SlabBlock} already tracks
 * BOTTOM/TOP/DOUBLE via its {@code TYPE} block-state property and already implements the
 * right-click-to-combine-into-a-double-slab behavior CE's {@code ItemSlab}/{@code isDouble} pair
 * hand-rolled - so this port collapses both CE classes into the one reusable vanilla-shaped
 * subclass every material's slab is built from, with a single registry entry per material instead
 * of a single+double pair.
 * <p>
 * This class is infrastructure only: no instances are registered from this package, since pairing
 * a slab with its full material block is generative content owned by whichever area builds those
 * base blocks (see the accompanying port report for the full list of CE materials still needing a
 * base block before a slab/stairs pair can be instantiated for them).
 */
public class BlockGenericSlab extends SlabBlock {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    public BlockGenericSlab(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
