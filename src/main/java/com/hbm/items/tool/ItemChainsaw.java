package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.world.item.Tier;

/**
 * Concrete axe-type fueled tool. Ported from CE's {@code com.hbm.items.tool.ItemChainsaw}.
 *
 * <p>CE's chainsaw additionally implements {@code IAnimatedItem} to drive a custom bone/bus swing
 * animation ({@code BusAnimation}/{@code HbmAnimations}) on every swing. That is a client-rendering
 * concern with no gameplay effect, and its supporting classes are not part of this port - deferred,
 * not silently dropped: the chainsaw functions identically to CE otherwise (fueled axe, silk touch
 * + vein-mining abilities, beheader weapon ability), it just swings with the plain vanilla item
 * animation instead of CE's custom one.
 */
public class ItemChainsaw extends ItemToolAbilityFueled {

    public ItemChainsaw(Properties properties, Tier tier, int maxFuel, int consumption, int fillRate, FluidType... acceptedFuels) {
        super(properties, tier, ToolRole.AXE, maxFuel, consumption, fillRate, acceptedFuels);
    }
}
