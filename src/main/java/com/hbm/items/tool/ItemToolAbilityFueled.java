package com.hbm.items.tool;

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
 * Adds a fluid-fuel tank on top of {@link ItemToolAbility} (used by {@link ItemChainsaw}). Ported
 * from CE's {@code com.hbm.items.tool.ItemToolAbilityFueled}.
 *
 * <p>Fuel is stored as a plain {@code Integer} data component ({@link ToolDataComponents#TOOL_FUEL})
 * rather than the {@code com.hbm.api.fluidmk2.IFillableItem} CE implements: that interface does not
 * exist anywhere in this port yet (a genuinely not-yet-ported Phase 2 fluid-item-network system),
 * so this class cannot be filled from pipes/tanks/canisters today. The fuel-consumption gameplay
 * (tool refuses to operate below {@link #consumption}, drains on every harvest) is fully
 * self-contained and works; only network refilling is deferred. {@link #tryFill} is kept as the
 * seam a future {@code IFillableItem} port should call into.
 */
public class ItemToolAbilityFueled extends ItemToolAbility {

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

    /** @return the leftover amount that could not be accepted (mirrors CE's {@code IFillableItem.tryFill}). */
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type)) {
            return amount;
        }

        int toFill = Math.min(amount, fillRate);
        toFill = Math.min(toFill, maxFuel - getFuel(stack));
        setFuel(stack, getFuel(stack) + toFill);

        return amount - toFill;
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
