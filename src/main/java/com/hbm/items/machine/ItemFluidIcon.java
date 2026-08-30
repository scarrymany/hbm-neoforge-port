package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure display/GUI helper item representing a fluid amount (plus optional pressure), never a real
 * tank a machine drains from. CE modeled this as one item with a metadata-selected fluid and
 * "fill"/"pressure" NBT; the fluid type doesn't back a fixed CE enum (the fluid registry keeps
 * growing), so per the porting plan this stays a single registered item with fluid id, fill and
 * pressure stored as data components instead of being flattened per fluid.
 */
public class ItemFluidIcon extends ItemBase {

    public ItemFluidIcon(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int quantity = getQuantity(stack);
        int pressure = getPressure(stack);
        if (quantity > 0) tooltip.add(Component.literal(quantity + "mB"));
        if (pressure > 0) tooltip.add(Component.literal(pressure + "PU").withStyle(ChatFormatting.RED));

        FluidType type = getFluidType(stack);
        if (type != null) type.addInfo(tooltip);
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidType type = getFluidType(stack);
        return type != null ? type.getLocalizedName() : super.getName(stack);
    }

    public static ItemStack addQuantity(ItemStack stack, int amount) {
        if (amount > 0) stack.set(MachineDataComponents.FLUID_AMOUNT.get(), amount);
        return stack;
    }

    public static ItemStack addPressure(ItemStack stack, int pressure) {
        stack.set(MachineDataComponents.FLUID_PRESSURE.get(), pressure);
        return stack;
    }

    public static ItemStack make(net.minecraft.world.item.Item fluidIconItem, FluidStack stack) {
        return make(fluidIconItem, stack.type, stack.fill, stack.pressure);
    }

    public static ItemStack make(net.minecraft.world.item.Item fluidIconItem, FluidType fluid, int fill) {
        return make(fluidIconItem, fluid, fill, 0);
    }

    public static ItemStack make(net.minecraft.world.item.Item fluidIconItem, FluidType fluid, int fill, int pressure) {
        ItemStack stack = new ItemStack(fluidIconItem);
        stack.set(MachineDataComponents.FLUID_ID.get(), fluid.getID());
        return addPressure(addQuantity(stack, fill), pressure);
    }

    public static int getQuantity(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_AMOUNT.get(), 0);
    }

    public static int getPressure(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_PRESSURE.get(), 0);
    }

    @Nullable
    public static FluidType getFluidType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemFluidIcon)) return null;
        Integer id = stack.get(MachineDataComponents.FLUID_ID.get());
        return id == null ? null : Fluids.fromID(id);
    }
}
