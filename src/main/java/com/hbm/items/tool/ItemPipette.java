package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemBase;
import com.hbm.items.machine.MachineDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Small precision fluid transfer tool - fills from one source, empties into another, one fluid at a
 * time. Ported from CE's {@code com.hbm.items.tool.ItemPipette} ({@code pipette},
 * {@code pipette_boron}, {@code pipette_laboratory}).
 *
 * <p>CE implemented {@code com.hbm.api.fluidmk2.IFillableItem} and hand-baked a filled/empty dual
 * item model plus a right-click sneak/no-sneak capacity-adjustment UX. Neither survives here:
 * {@code IFillableItem} does not exist anywhere in this port yet (see
 * {@link ItemToolAbilityFueled}'s javadoc for the same finding), and there is no fluid-item-network
 * to fill from/drain into regardless, so the capacity-adjustment interaction (a CE-only UX quirk with
 * no gameplay effect until such a network exists) is dropped rather than ported. What is kept is the
 * real, useful part: fluid type + fill level persisted per stack (via the shared
 * {@link MachineDataComponents#FLUID_ID}/{@link MachineDataComponents#FLUID_AMOUNT} components,
 * matching {@link ItemCanister}/{@link ItemGasCanister}'s precedent), a fixed max capacity per
 * variant (CE: 50mB for {@code pipette_laboratory}, 1000mB for the other two), and the
 * accept/fill/drain methods a future fluid-item-network port should call into, named the same as
 * CE's own {@code IFillableItem} methods so that migration is a drop-in interface implementation
 * later rather than a rewrite.
 */
public class ItemPipette extends ItemBase {

    private final int maxFill;
    private final boolean corrosiveFizzles;

    public ItemPipette(Properties properties, int maxFill, boolean corrosiveFizzles) {
        super(properties);
        this.maxFill = maxFill;
        this.corrosiveFizzles = corrosiveFizzles;
    }

    public int getMaxFill() {
        return this.maxFill;
    }

    public static FluidType getType(ItemStack stack) {
        Integer id = stack.get(MachineDataComponents.FLUID_ID.get());
        return id == null ? Fluids.NONE : Fluids.fromID(id);
    }

    public static int getFill(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_AMOUNT.get(), 0);
    }

    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        if (type == null || type.isAntimatter()) {
            return false;
        }
        FluidType current = getType(stack);
        return current == Fluids.NONE || current == type || getFill(stack) == 0;
    }

    /** @return the leftover amount that could not be accepted (mirrors CE's {@code IFillableItem.tryFill}). */
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (amount <= 0 || !acceptsFluid(type, stack)) {
            return amount;
        }

        int fill = getFill(stack);
        int toFill = Math.min(amount, this.maxFill - fill);
        if (toFill <= 0) {
            return amount;
        }

        stack.set(MachineDataComponents.FLUID_ID.get(), type.getID());
        stack.set(MachineDataComponents.FLUID_AMOUNT.get(), fill + toFill);

        if (this.corrosiveFizzles && willFizzle(type)) {
            stack.shrink(stack.getCount());
        }

        return amount - toFill;
    }

    public boolean willFizzle(FluidType type) {
        return this.corrosiveFizzles && type.isCorrosive() && type != Fluids.PEROXIDE;
    }

    public boolean providesFluid(FluidType type, ItemStack stack) {
        return getType(stack) == type && getFill(stack) > 0;
    }

    /**
     * @return the amount actually drained, capped at {@code amount} (mirrors CE's {@code
     *     IFillableItem.tryEmpty}: "provides fluid with the maximum being the requested amount"),
     *     or the full {@code amount} unchanged if nothing could be drained.
     */
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (amount <= 0 || !providesFluid(type, stack)) {
            return amount;
        }

        int fill = getFill(stack);
        int toDrain = Math.min(amount, fill);
        int remaining = fill - toDrain;

        if (remaining <= 0) {
            stack.set(MachineDataComponents.FLUID_ID.get(), Fluids.NONE.getID());
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), 0);
        } else {
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), remaining);
        }

        return toDrain;
    }

    public FluidType getFirstFluidType(ItemStack stack) {
        return getType(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidType type = getType(stack);
        tooltip.add(Component.literal("Fluid: ").append(type.getLocalizedName()));
        tooltip.add(Component.literal("Amount: " + getFill(stack) + "/" + this.maxFill + "mB"));
    }
}
