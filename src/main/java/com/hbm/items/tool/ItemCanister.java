package com.hbm.items.tool;

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
 * Fill/drain fluid canister. Ported from CE's {@code com.hbm.items.tool.ItemCanister}
 * ({@code canister_fuel}/{@code canister_full}).
 *
 * <p>CE modeled "one canister item, one metadata variant per fluid" with a hand-baked dual-layer
 * item model (base + fluid-tinted overlay) rebuilt at {@code ModelBakeEvent} time. 1.21 has no
 * metadata and this port has no equivalent custom item-model-baking pipeline, so - following the
 * exact precedent already established by {@link com.hbm.items.machine.ItemFluidTank} in this same
 * port for the same underlying shape (a fillable container that displays whatever fluid it holds) -
 * this class stores its fluid type and fill level as the shared
 * {@link MachineDataComponents#FLUID_ID}/{@link MachineDataComponents#FLUID_AMOUNT} components
 * instead, with one registered item per canister capacity rather than per fluid. Actual filling from
 * pipes/tanks is not wired up: CE resolved that through {@code FluidContainerRegistry}, which -
 * same as {@code ItemFluidTank}'s own javadoc already documents - has not been ported yet. The
 * {@link #tryFill}/{@link #tryEmpty} methods below are the seam a future fluid-item-network port
 * should call into.
 */
public class ItemCanister extends ItemBase {

    private final int capacity;

    public ItemCanister(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Nullable
    public static FluidType getFluidType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemCanister)) return null;
        Integer id = stack.get(MachineDataComponents.FLUID_ID.get());
        return id == null ? null : Fluids.fromID(id);
    }

    public static int getFill(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_AMOUNT.get(), 0);
    }

    /** @return the leftover amount that could not be accepted. */
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (amount <= 0 || type == null || type == Fluids.NONE || type.isAntimatter()) {
            return amount;
        }

        FluidType current = getFluidType(stack);
        if (current != null && current != Fluids.NONE && current != type) {
            return amount;
        }

        int fill = getFill(stack);
        int toFill = Math.min(amount, this.capacity - fill);
        if (toFill <= 0) {
            return amount;
        }

        stack.set(MachineDataComponents.FLUID_ID.get(), type.getID());
        stack.set(MachineDataComponents.FLUID_AMOUNT.get(), fill + toFill);
        return amount - toFill;
    }

    /** @return the leftover amount that could not be drained. */
    public int tryEmpty(int amount, ItemStack stack) {
        int fill = getFill(stack);
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
        int fill = getFill(stack);
        tooltip.add(Component.literal(fill + "/" + this.capacity + " mB"));
        if (type != null && type != Fluids.NONE) {
            tooltip.add(type.getLocalizedName());
        }
    }
}
