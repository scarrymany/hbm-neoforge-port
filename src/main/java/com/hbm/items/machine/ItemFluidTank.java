package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fill/empty fluid container. CE resolved fill/drain through {@code FluidContainerRegistry}, a
 * Forge-era data registry that is not part of this area's scope and has not been ported yet -
 * populating that registry (and wiring the matching item fluid-handler capability, already scaffolded
 * in {@code com.hbm.capability.NTMFluidCapabilityHandler}/{@code NTMFluidContainerWrapper}) is a
 * follow-up once that registry exists. This class carries the item's own state and display logic:
 * fluid type and fill amount as data components, one registered item per tank size (matching CE's
 * own per-size instantiation - {@code fluid_tank_full}, {@code fluid_tank_lead_full},
 * {@code fluid_barrel_full}, {@code fluid_pack_full}), rather than per fluid.
 */
public class ItemFluidTank extends ItemBase {

    private final int capacity;

    public ItemFluidTank(int capacity, Properties properties) {
        super(properties.stacksTo(64));
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Nullable
    public static FluidType getFluidType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemFluidTank)) return null;
        Integer id = stack.get(MachineDataComponents.FLUID_ID.get());
        return id == null ? null : Fluids.fromID(id);
    }

    public static int getFill(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_AMOUNT.get(), 0);
    }

    public static ItemStack fill(ItemStack stack, FluidType type, int amount) {
        stack.set(MachineDataComponents.FLUID_ID.get(), type.getID());
        stack.set(MachineDataComponents.FLUID_AMOUNT.get(), amount);
        return stack;
    }

    public static ItemStack empty(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(MachineDataComponents.FLUID_ID.get());
        copy.remove(MachineDataComponents.FLUID_AMOUNT.get());
        return copy;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidType type = getFluidType(stack);
        return type != null ? type.getLocalizedName() : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidType type = getFluidType(stack);
        int fill = getFill(stack);
        String line = fill + "/" + this.capacity + " mB";
        if (stack.getCount() > 1) line = stack.getCount() + "x " + line;
        tooltip.add(Component.literal(line));
        if (type != null) type.addInfoItemTanks(tooltip);
    }
}
