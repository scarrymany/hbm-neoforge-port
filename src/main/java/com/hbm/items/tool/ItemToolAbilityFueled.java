package com.hbm.items.tool;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

/**
 * Exact CE {@code ItemToolAbilityFueled} — {@link IFillableItem} tank on {@link ItemToolAbility}.
 * Fuel in {@link ToolDataComponents#TOOL_FUEL}. Machines fill via {@code FluidLoaderFillableItem}.
 */
public class ItemToolAbilityFueled extends ItemToolAbility implements IFillableItem {

    protected final int maxFuel;
    protected final int consumption;
    protected final int fillRate;
    protected final Set<FluidType> acceptedFuels;

    public ItemToolAbilityFueled(Properties properties, Tier tier, ToolRole role, int maxFuel, int consumption, int fillRate, FluidType... acceptedFuels) {
        super(properties, tier, role);
        this.maxFuel = maxFuel;
        this.consumption = consumption;
        this.fillRate = fillRate;
        this.acceptedFuels = Set.of(acceptedFuels);
    }

    public int getFuel(ItemStack stack) {
        return stack.getOrDefault(ToolDataComponents.TOOL_FUEL.get(), maxFuel);
    }

    public void setFuel(ItemStack stack, int fuel) {
        stack.set(ToolDataComponents.TOOL_FUEL.get(), Math.clamp(fuel, 0, maxFuel));
    }

    public boolean acceptsFluid(FluidType type) {
        return acceptedFuels.contains(type);
    }

    /** Exact CE {@code ItemToolAbilityFueled.java}:92-94. */
    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return acceptsFluid(type);
    }

    /** Exact CE {@code :97-107} — remainder; fill capped at {@code fillRate} then tank space. */
    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;
        int toFill = Math.min(amount, this.fillRate);
        toFill = Math.min(toFill, this.maxFuel - getFill(stack));
        setFuel(stack, getFill(stack) + toFill);
        return amount - toFill;
    }

    /** Exact CE {@code :110-112}. */
    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    /** Exact CE {@code :115-117} — leftover unchanged. */
    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return amount;
    }

    /** Exact CE {@code :127-129}. */
    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return null;
    }

    /** Exact CE {@code :73-81} — {@code TOOL_FUEL}, default full. */
    @Override
    public int getFill(ItemStack stack) {
        return getFuel(stack);
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return getFuel(stack) >= consumption;
    }

    @Override
    protected void applyWear(ItemStack stack, Player player, int amount) {
        setFuel(stack, Math.max(getFuel(stack) - amount * consumption, 0));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFuel(stack) < maxFuel;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFuel(stack) / maxFuel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Fuel: " + getFuel(stack) + "/" + maxFuel + "mB").withStyle(ChatFormatting.GOLD));

        for (FluidType type : acceptedFuels) {
            tooltipComponents.add(Component.literal("- ").withStyle(ChatFormatting.YELLOW).append(type.getLocalizedName()));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
