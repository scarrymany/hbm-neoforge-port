package com.hbm.items.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Research reactor plate fuel: {@link ItemFuelRod} plus a reactivity function, entirely static
 * over the ItemStack, no tile entity reference.
 */
public class ItemPlateFuel extends ItemFuelRod {

    private final FunctionEnum function;
    private final int reactivity;

    public ItemPlateFuel(int lifeTime, FunctionEnum function, int reactivity, Properties properties) {
        super(lifeTime, properties);
        this.function = function;
        this.reactivity = reactivity;
    }

    public int react(ItemStack stack, int flux) {
        if (this.function != FunctionEnum.PASSIVE) {
            setLifeTime(stack, getLifeTime(stack) + flux);
        }

        return switch (this.function) {
            case LOGARITHM -> (int) (Math.log10(flux + 1) * 0.5D * this.reactivity);
            case SQUARE_ROOT -> (int) (Math.sqrt(flux) * this.reactivity / 10D);
            case NEGATIVE_QUADRATIC -> (int) Math.max((flux - (flux * (double) flux / 10000D)) / 100D * this.reactivity, 0);
            case LINEAR -> (int) (flux / 100D * this.reactivity);
            case PASSIVE -> {
                setLifeTime(stack, getLifeTime(stack) + this.reactivity);
                yield this.reactivity;
            }
        };
    }

    public String getFunctionDesc() {
        return switch (this.function) {
            case LOGARITHM -> "f(x) = log10(x + 1) * 0.5 * " + this.reactivity;
            case SQUARE_ROOT -> "f(x) = sqrt(x) * " + this.reactivity + " / 10";
            case NEGATIVE_QUADRATIC -> "f(x) = [x - (x² / 10000)] / 100 * " + this.reactivity;
            case LINEAR -> "f(x) = x / 100 * " + this.reactivity;
            case PASSIVE -> "f(x) = " + this.reactivity;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String lifetime = MachineMathUtil.getShortNumber(getLifeTime());
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "[Research Reactor Plate Fuel]"));
        tooltip.add(Component.literal(ChatFormatting.DARK_AQUA + "   " + getFunctionDesc()));
        tooltip.add(Component.literal(ChatFormatting.DARK_AQUA + "   Yield of " + lifetime + " events"));
    }

    public enum FunctionEnum {
        LOGARITHM,
        SQUARE_ROOT,
        NEGATIVE_QUADRATIC,
        LINEAR,
        PASSIVE
    }
}
