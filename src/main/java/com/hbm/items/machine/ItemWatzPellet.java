package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Watz isotropic fuel, oxidized. Complex enrichment math, entirely static over the ItemStack, no
 * tile entity reference. CE's twelve metadata grades times two base items (fresh/depleted) become
 * twenty-four registered instances; only the fresh item tracks/reports a live enrichment yield
 * (matching CE's own {@code this != ModItems.watz_pellet} guard), the depleted item is inert.
 */
public class ItemWatzPellet extends ItemBase {

    private final EnumWatzType type;
    private final boolean depleted;

    public ItemWatzPellet(EnumWatzType type, boolean depleted, Properties properties) {
        super(properties.stacksTo(16));
        this.type = type;
        this.depleted = depleted;
    }

    public EnumWatzType getType() {
        return this.type;
    }

    public static double getYield(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemWatzPellet pellet)) return 0D;
        return stack.getOrDefault(MachineDataComponents.WATZ_YIELD.get(), pellet.type.yield);
    }

    public static void setYield(ItemStack stack, double yield) {
        stack.set(MachineDataComponents.WATZ_YIELD.get(), yield);
    }

    public static double getEnrichment(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemWatzPellet pellet)) return 0D;
        return getYield(stack) / pellet.type.yield;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !this.depleted && getDurabilityForDisplay(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (1F - (float) getEnrichment(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x32FFFF;
    }

    private double getDurabilityForDisplay(ItemStack stack) {
        return 1D - getEnrichment(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.depleted) return;

        tooltip.add(Component.literal(ChatFormatting.GREEN + "Depletion: " + String.format(Locale.US, "%.1f", getDurabilityForDisplay(stack) * 100D) + "%"));

        String color = ChatFormatting.GOLD.toString();
        String reset = ChatFormatting.RESET.toString();

        if (this.type.passive > 0) {
            tooltip.add(Component.literal(color + "Base fission rate: " + reset + this.type.passive));
            tooltip.add(Component.literal(ChatFormatting.RED + "Self-igniting!"));
        }
        if (this.type.heatEmission > 0) tooltip.add(Component.literal(color + "Heat per flux: " + reset + this.type.heatEmission + " TU"));
        if (this.type.burnFunc != null) {
            tooltip.add(Component.literal(color + "Reaction function: " + reset + this.type.burnFunc.getLabelForFuel()));
            tooltip.add(Component.literal(color + "Fuel type: " + reset + this.type.burnFunc.getDangerFromFuel()));
        }
        if (this.type.heatDiv != null) tooltip.add(Component.literal(color + "Thermal multiplier: " + reset + this.type.heatDiv.getLabelForFuel() + " TU⁻¹"));
        if (this.type.absorbFunc != null) tooltip.add(Component.literal(color + "Flux capture: " + reset + this.type.absorbFunc.getLabelForFuel()));
    }

    public enum EnumWatzType {
        SCHRABIDIUM(2_000, 20D, new FuelReactivityFunction.Linear(1.5D), new FuelReactivityFunction.SqrtFalling(10D), null),
        HES(1_750, 20D, new FuelReactivityFunction.Linear(1.25D), new FuelReactivityFunction.SqrtFalling(15D), null),
        MES(1_500, 15D, new FuelReactivityFunction.Linear(1.15D), new FuelReactivityFunction.SqrtFalling(15D), null),
        LES(1_250, 15D, new FuelReactivityFunction.Linear(1D), new FuelReactivityFunction.SqrtFalling(20D), null),
        HEN(0, 10D, new FuelReactivityFunction.Sqrt(100), new FuelReactivityFunction.SqrtFalling(10D), null),
        MEU(0, 10D, new FuelReactivityFunction.Sqrt(75), new FuelReactivityFunction.SqrtFalling(10D), null),
        MEP(0, 15D, new FuelReactivityFunction.Sqrt(150), new FuelReactivityFunction.SqrtFalling(10D), null),
        LEAD(0, 0, null, null, new FuelReactivityFunction.Sqrt(10)),
        BORON(0, 0, null, null, new FuelReactivityFunction.Linear(10)),
        DU(0, 0, null, null, new FuelReactivityFunction.Quadratic(1D, 1D).withDiv(100)),
        NQD(2_000, 20, new FuelReactivityFunction.Linear(2D), new FuelReactivityFunction.Sqrt(1D / 25D).withOff(25D * 25D), null),
        NQR(2_500, 30, new FuelReactivityFunction.Linear(1.5D), new FuelReactivityFunction.Sqrt(1D / 25D).withOff(25D * 25D), null);

        public static final EnumWatzType[] VALUES = values();

        public final double passive;
        public final double heatEmission;
        public final FuelReactivityFunction burnFunc;
        public final FuelReactivityFunction heatDiv;
        public final FuelReactivityFunction absorbFunc;
        public final double yield = 500_000_000;

        EnumWatzType(double passive, double heatEmission, FuelReactivityFunction burnFunc, FuelReactivityFunction heatDiv, FuelReactivityFunction absorbFunc) {
            this.passive = passive;
            this.heatEmission = heatEmission;
            this.burnFunc = burnFunc;
            this.heatDiv = heatDiv;
            this.absorbFunc = absorbFunc;
        }
    }
}
