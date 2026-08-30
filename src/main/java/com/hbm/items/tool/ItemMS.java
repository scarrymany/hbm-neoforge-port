package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * "Meteor sample" tool: breaks a specific world-gen block and drops unique ingots. Ported from
 * CE's {@code com.hbm.items.tool.ItemMS} ({@code mysteryshovel}).
 *
 * <p><b>Stubbed pending {@code ModBlocks.ntm_dirt}.</b> CE's {@code onItemUse} checks the clicked
 * block against {@code ModBlocks.ntm_dirt} (a world-gen-only block) and, on a match, destroys it and
 * drops three {@code ingot_u238m2} variants. No {@code ntm_dirt} block (or {@code ModItems.ingot_u238m2})
 * exists anywhere in this port yet - {@code com.hbm.blocks.ModBlocks} is still the Phase 0 registry
 * skeleton. Per the port plan's "stub with a documented TODO rather than blocking" rule, the item is
 * registered (tooltip included) with its use-behavior left a no-op {@link InteractionResult#PASS}.
 */
public class ItemMS extends Item {

    public ItemMS(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Lost but not forgotten"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once ModBlocks.ntm_dirt and ModItems.ingot_u238m2 exist, port
        // CE's behavior here - on a server-side hit against ntm_dirt, destroy the block and spawn
        // three ingot_u238m2 variant item entities with CE's randomized velocity/offset.
        return InteractionResult.PASS;
    }
}
