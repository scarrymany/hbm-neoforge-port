package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Rebar-construction placement tool, ported from CE's {@code com.hbm.items.tool.ItemRebarPlacer}.
 * <p>
 * <b>Stubbed pending {@code com.hbm.blocks.generic.BlockRebar} and
 * {@code com.hbm.uninos.networkproviders.RebarNetwork}.</b> Confirmed via direct CE directory search
 * and a repo-wide grep of this port: neither {@code BlockRebar} nor {@code RebarNetwork} exists
 * anywhere in this port, and none of this wave's other Phase 2 research packages claim them (per
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Deferred scope, which
 * recommends "a small dedicated construction/rebar Phase 2 (or late-Phase-2) research package"). This
 * also depends on {@code com.hbm.uninos}'s generic network-provider layer being extended with a
 * concrete {@code RebarNetwork} implementation, which Phase 0 did not port either. Per the port
 * plan's "stub with a documented TODO rather than blocking" rule, the item is registered (tooltip
 * included) with its use-behavior left a no-op {@link InteractionResult#PASS}.
 */
public class ItemRebarPlacer extends Item {

    public ItemRebarPlacer(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Places rebar for reinforced concrete construction."));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once com.hbm.blocks.generic.BlockRebar and
        // com.hbm.uninos.networkproviders.RebarNetwork exist, port CE's rebar-placement/
        // ContainerRebar/GUIRebar behavior here.
        return InteractionResult.PASS;
    }
}
