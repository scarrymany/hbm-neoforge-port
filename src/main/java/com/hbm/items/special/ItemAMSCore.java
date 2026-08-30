package com.hbm.items.special;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemAMSCore}: a fusion-reactor fuel-core data item whose power/heat/fuel
 * stats are baked into distinct instances (not metadata), backing {@code ams_core_sing},
 * {@code ams_core_wormhole}, {@code ams_core_eyeofharmony} and {@code ams_core_thingy}. Fully
 * functional standalone (registration, tooltip, stat accessors); it is only meaningless without
 * the fusion reactor multiblock, which is Phase 2 scope.
 * <p>
 * CE dispatched its four items' flavor text and rarity via {@code this ==
 * ModItems.ams_core_<name>} identity checks inside one shared {@code addInformation}. The port
 * carries the flavor text per-instance instead (via the constructor) and sets rarity through
 * {@code Item.Properties} at registration - both are equivalent to CE's behavior without the
 * item class needing to know about specific {@code ModItems} fields. Not ported: the
 * {@code ams_core_thingy} enchantment-glint/alternate-flavor Easter egg gated on CE's
 * {@code MainRegistry.polaroidID}, which has no equivalent counter in the port yet.
 */
public class ItemAMSCore extends Item {

    private final int powerBase;
    private final float heatBase;
    private final float fuelBase;
    private final List<Component> flavorText;

    public ItemAMSCore(Properties properties, int powerBase, float heatBase, float fuelBase, List<Component> flavorText) {
        super(properties);
        this.powerBase = powerBase;
        this.heatBase = heatBase;
        this.fuelBase = fuelBase;
        this.flavorText = flavorText;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(flavorText);
        tooltip.add(Component.literal("[DFC Core]").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(statLine("Power: " + powerBase));
        tooltip.add(statLine("Heat: " + (heatBase > 1 ? "+" : "") + formatPercent(heatBase)));
        tooltip.add(statLine("Fuel: " + (fuelBase > 1 ? "+" : "") + formatPercent(fuelBase)));
    }

    private static Component statLine(String text) {
        return Component.literal(" " + text).withStyle(ChatFormatting.AQUA);
    }

    private static String formatPercent(float multiplier) {
        return (Math.round(multiplier * 1000) * 0.10 - 100) + "%";
    }

    public static int getPowerBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.powerBase : 0;
    }

    public static float getHeatBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.heatBase : 1F;
    }

    public static float getFuelBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.fuelBase : 1F;
    }
}
