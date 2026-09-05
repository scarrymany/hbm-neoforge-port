package com.hbm.items.tool;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemBase;
import com.hbm.items.machine.MachineDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fixed-capacity (1000mB) gas-tank canister. Ported from CE's
 * {@code com.hbm.items.tool.ItemGasCanister} ({@code gas_full}).
 *
 * <p>Same data-component-based simplification as {@link ItemCanister} - see that class's javadoc
 * for the full rationale (CE's per-fluid metadata + dual-color-layer baked model has no 1.21
 * equivalent here; this stores fluid type/fill via the shared
 * {@link MachineDataComponents#FLUID_ID}/{@link MachineDataComponents#FLUID_AMOUNT} components
 * instead). Unlike {@link ItemCanister}, CE hardcoded this container's capacity to 1000mB rather
 * than taking it as a constructor argument, so this class does the same.
 */
public class ItemGasCanister extends ItemBase implements IFillableItem {

    public static final int CAPACITY = 1000;

    public ItemGasCanister(Properties properties) {
        super(properties);
    }

    @Nullable
    public static FluidType getFluidType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGasCanister)) return null;
        Integer id = stack.get(MachineDataComponents.FLUID_ID.get());
        return id == null ? null : Fluids.fromID(id);
    }

    public static int fillOf(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_AMOUNT.get(), 0);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        if (type == null || type == Fluids.NONE || type.isAntimatter()) return false;
        FluidType current = getFluidType(stack);
        return current == null || current == Fluids.NONE || current == type;
    }

    /** @return the leftover amount that could not be accepted. */
    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (amount <= 0 || type == null || type == Fluids.NONE || type.isAntimatter()) {
            return amount;
        }

        FluidType current = getFluidType(stack);
        if (current != null && current != Fluids.NONE && current != type) {
            return amount;
        }

        int fill = fillOf(stack);
        int toFill = Math.min(amount, CAPACITY - fill);
        if (toFill <= 0) {
            return amount;
        }

        stack.set(MachineDataComponents.FLUID_ID.get(), type.getID());
        stack.set(MachineDataComponents.FLUID_AMOUNT.get(), fill + toFill);
        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return type != null && type == getFluidType(stack) && fillOf(stack) > 0;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (!providesFluid(type, stack) || amount <= 0) return 0;
        int fill = fillOf(stack);
        int moved = Math.min(fill, amount);
        int leftover = tryEmpty(moved, stack);
        return moved - leftover;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return getFluidType(stack);
    }

    @Override
    public int getFill(ItemStack stack) {
        return fillOf(stack);
    }

    /** @return the leftover amount that could not be drained. */
    public int tryEmpty(int amount, ItemStack stack) {
        int fill = fillOf(stack);
        int toDrain = Math.min(amount, fill);
        if (toDrain <= 0) {
            return amount;
        }

        int remaining = fill - toDrain;
        if (remaining <= 0) {
            stack.remove(MachineDataComponents.FLUID_ID.get());
            stack.remove(MachineDataComponents.FLUID_AMOUNT.get());
        } else {
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), remaining);
        }
        return amount - toDrain;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidType type = getFluidType(stack);
        if (type != null && type != Fluids.NONE) {
            return Component.translatable(this.getDescriptionId(stack)).append(" ").append(type.getLocalizedName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidType type = getFluidType(stack);
        int fill = fillOf(stack);
        tooltip.add(Component.literal(fill + "/" + CAPACITY + " mB"));
        if (type != null && type != Fluids.NONE) {
            tooltip.add(type.getLocalizedName());
        }
    }
}
