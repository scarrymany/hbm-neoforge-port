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
 * Fluid tank backed by a real fluid-handler-item capability in CE (already the modern-shaped
 * design - NeoForge's {@code IFluidHandlerItem} capability is the direct successor of Forge's).
 * The item's own state (fluid type + fill amount as data components, one registered item per
 * size) is fully self-contained here; attaching the actual
 * {@code Capabilities.FluidHandler.ITEM} capability provider (reading/writing the same two
 * components this class exposes) is a follow-up for whichever phase wires
 * {@code com.hbm.capability.ModCapabilities} up to these size-only tank items, since that
 * registration currently only covers {@code NTMFluidCapabilityHandler.isNtmFluidContainer} items
 * (the {@code FluidContainerRegistry}-based classic containers), not this component-based design.
 */
public class ItemFluidTankV2 extends ItemBase {

    private final int capacity;

    public ItemFluidTankV2(int capacity, Properties properties) {
        super(properties.stacksTo(64));
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Nullable
    public static FluidType getFluidType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemFluidTankV2)) return null;
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
    public boolean isBarVisible(ItemStack stack) {
        int amount = getFill(stack);
        return amount != this.capacity && amount != 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFill(stack) / (float) this.capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FluidType type = getFluidType(stack);
        return type != null ? type.getColor() : 0xFFFFFF;
    }

    @Override
    public Component getName(ItemStack stack) {
        int amount = getFill(stack);
        FluidType type = getFluidType(stack);
        if (amount == 0 || type == null) return super.getName(stack);
        return Component.translatable(this.getDescriptionId() + ".not_empty", type.getLocalizedName());
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
