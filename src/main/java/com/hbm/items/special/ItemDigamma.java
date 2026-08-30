package com.hbm.items.special;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemDigamma} ({@code particle_digamma}). {@code digamma} is CE's per-instance
 * half-life parameter in ticks (note CE's own comment: for this class it means "ticks until half
 * life", the inverse of how the superclass' generic digamma-hazard interpretation reads a flat
 * value).
 * <p>
 * Not ported: {@code ContaminationUtil.applyDigammaData} (CE's per-tick player contamination
 * accumulator, which would otherwise run from an {@code inventoryTick} override) -
 * {@code com.hbm.util.ContaminationUtil} has not been ported by any Phase 1 area yet; and the
 * dropped-item {@code EntityQuasar} spawn (no entity system ported through Phase 1, see
 * docs/phase1/items_special.md finding 4's sibling systems). CE's own hazard table binds no static
 * entry for this item at all (verified against
 * {@code upstream/hbm-ce/.../hazard/HazardRegistry.java}: no {@code particle_digamma} call exists
 * there) - its radiation entirely comes from the deferred {@code ContaminationUtil} call, not
 * {@code HazardSystem.register(...)}, so no hazard binding is added for it here either.
 */
public class ItemDigamma extends ItemBase {

    private final int digamma;

    public ItemDigamma(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Half-life (particle): 1.67*10^34 a").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Half-life (holder): " + (digamma / 20.0) + "s").withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("[Dangerous Drop]").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // EntityQuasar spawn-on-drop deferred - see class javadoc.
        return false;
    }
}
