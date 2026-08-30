package com.hbm.items.armor;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.JetpackFueledBase} (92 lines) - {@link JetpackBase} +
 * {@link IFillableItem}: a flat {@code int} mB amount of one fixed accepted {@link FluidType},
 * exactly like {@link ArmorFSBFueled}'s shape but backed by {@link JetpackBase#getFuel}/
 * {@link JetpackBase#setFuel} (the jetpack fuel component) rather than
 * {@link ArmorDataComponents#ARMOR_FUEL}. All 4 non-Glider jetpacks ({@code Jetpack{Regular,Break,
 * Booster,Vectorized}}, {@code com.hbm.items.gear}) extend this class.
 */
public abstract class JetpackFueledBase extends JetpackBase implements IFillableItem {

    public final FluidType fuelType;
    public final int maxFuel;

    public JetpackFueledBase(Item.Properties properties, FluidType fuelType, int maxFuel) {
        super(properties);
        this.fuelType = fuelType;
        this.maxFuel = maxFuel;
    }

    public int getMaxFill(ItemStack stack) {
        return this.maxFuel;
    }

    public int getLoadSpeed(ItemStack stack) {
        return 10;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == this.fuelType;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;

        int fill = getFuel(stack);
        int toFill = Math.min(amount, maxFuel - fill);
        if (toFill <= 0) return amount;

        setFuel(stack, fill + toFill);
        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return 0;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return fuelType;
    }

    @Override
    public int getFill(ItemStack stack) {
        return getFuel(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.empty().append(this.fuelType.getLocalizedName())
                .append(": " + getFuel(stack) + "mB / " + this.maxFuel + "mB").withStyle(ChatFormatting.LIGHT_PURPLE));
        components.add(Component.empty());
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        ItemStack jetpack = ArmorModHandler.pryMod(armor, ArmorModHandler.plate_only);
        if (jetpack.isEmpty()) return;

        list.add(Component.literal("  ").withStyle(ChatFormatting.RED).append(stack.getHoverName())
                .append(" (").append(fuelType.getLocalizedName()).append(": " + getFuel(jetpack) + "mB / " + this.maxFuel + "mB)"));
    }
}
