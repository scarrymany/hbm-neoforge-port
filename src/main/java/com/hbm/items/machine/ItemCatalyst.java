package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * AMS catalyst: plain stat-carrier item read by a (Phase 2) antimatter synthesis machine. Every
 * CE instance is its own registered item already (not a metadata variant), so this ports as a
 * single per-instance class exactly as CE had it.
 */
public class ItemCatalyst extends ItemBase {

    private final int color;
    private final long powerAbs;
    private final float powerMod;
    private final float heatMod;
    private final float fuelMod;

    public ItemCatalyst(int color, Properties properties) {
        this(color, 0, 1.0F, 1.0F, 1.0F, properties);
    }

    public ItemCatalyst(int color, long powerAbs, float powerMod, float heatMod, float fuelMod, Properties properties) {
        super(properties);
        this.color = color;
        this.powerAbs = powerAbs;
        this.powerMod = powerMod;
        this.heatMod = heatMod;
        this.fuelMod = fuelMod;
    }

    public int getColor() {
        return this.color;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Adds spice to the core."));
        tooltip.add(Component.literal("Look at all those colors!"));
    }

    public static long getPowerAbs(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.powerAbs : 0;
    }

    public static float getPowerMod(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.powerMod : 1F;
    }

    public static float getHeatMod(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.heatMod : 1F;
    }

    public static float getFuelMod(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.fuelMod : 1F;
    }
}
