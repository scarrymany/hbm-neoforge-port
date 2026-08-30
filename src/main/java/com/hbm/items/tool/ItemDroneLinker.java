package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Drone-network linking tool, ported from CE's {@code com.hbm.items.tool.ItemDroneLinker}.
 * <p>
 * <b>Stubbed pending {@code com.hbm.tileentity.network.IDroneLinkable}</b> - see {@link ItemDrone}'s
 * javadoc for the full drone-logistics-subsystem gap this item shares a root cause with. Per the
 * port plan's "stub with a documented TODO rather than blocking" rule, the item is registered
 * (tooltip included) with its use-behavior left a no-op {@link InteractionResult#PASS}.
 */
public class ItemDroneLinker extends Item {

    public ItemDroneLinker(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Links drone docks, waypoints, requesters and providers."));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once com.hbm.tileentity.network.IDroneLinkable (or this
        // port's blockentity-package equivalent) exists, port CE's mark-then-link behavior here.
        return InteractionResult.PASS;
    }
}
