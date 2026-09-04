package com.hbm.items.tool;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemBase;
import com.hbm.items.machine.MachineDataComponents;
import com.hbm.util.TagsUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exact CE {@code ItemPipette} — {@link IFillableItem} fill/empty + capacity click.
 * Overlay / dual-model stay skipped (no invent art).
 */
public class ItemPipette extends ItemBase implements IFillableItem {

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

    /** Exact CE {@code ItemPipette.java}:78-84 — NBT {@code capacity}, default {@code getMaxFill}. */
    public int getCapacity(ItemStack stack) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        if (!tag.contains("capacity")) return this.maxFill;
        return tag.getInt("capacity");
    }

    public void setCapacity(ItemStack stack, int capacity) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        tag.putInt("capacity", capacity);
        TagsUtil.putCustomData(stack, tag);
    }

    public FluidType getType(ItemStack stack) {
        Integer id = stack.get(MachineDataComponents.FLUID_ID.get());
        return id == null ? Fluids.NONE : Fluids.fromID(id);
    }

    /** Exact CE {@code :105-134} — empty: sneak/click capacity; filled: {@code desc.item.pipette.noEmpty}. */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (getFill(stack) == 0) {
            int capacity = getCapacity(stack);
            int updated;
            if (this.maxFill <= 50) {
                updated = player.isShiftKeyDown() ? Math.max(capacity - 1, 1) : Math.min(capacity + 1, this.maxFill);
            } else {
                updated = player.isShiftKeyDown() ? Math.max(capacity - 50, 50) : Math.min(capacity + 50, this.maxFill);
            }
            setCapacity(stack, updated);
            player.displayClientMessage(Component.literal(updated + "/" + this.maxFill + "mB"), false);
        } else {
            player.displayClientMessage(Component.translatable("desc.item.pipette.noEmpty"), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        if (type == null || type.isAntimatter()) return false;
        FluidType current = getType(stack);
        return (current == type || getFill(stack) == 0);
    }

    /** Exact CE {@code :164-191} — remainder; fill capped at capacity; fizzle after fill. */
    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (stack.isEmpty() || amount <= 0 || !acceptsFluid(type, stack)) return amount;
        if (getFill(stack) == 0) {
            stack.set(MachineDataComponents.FLUID_ID.get(), type.getID());
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), 0);
        }
        int capacity = getCapacity(stack);
        int fill = getFill(stack);
        int toFill = Math.min(Math.max(0, capacity - fill), amount);
        if (toFill > 0) {
            stack.set(MachineDataComponents.FLUID_ID.get(), type.getID());
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), fill + toFill);
        }
        if (getFill(stack) > 0 && willFizzle(type)) {
            stack.shrink(stack.getCount());
        }
        return amount - toFill;
    }

    public boolean willFizzle(FluidType type) {
        return this.corrosiveFizzles && type.isCorrosive() && type != Fluids.PEROXIDE;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return getType(stack) == type;
    }

    /** Amount moved (FluidLoader contract). CE fail path {@code return amount} not used — loaders treat return as moved. */
    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (stack.isEmpty() || amount <= 0 || !providesFluid(type, stack)) return 0;
        int stored = getFill(stack);
        int toDrain = Math.min(amount, stored);
        int remaining = stored - toDrain;
        if (remaining <= 0) {
            stack.set(MachineDataComponents.FLUID_ID.get(), Fluids.NONE.getID());
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), 0);
        } else {
            stack.set(MachineDataComponents.FLUID_AMOUNT.get(), remaining);
        }
        return toDrain;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return getType(stack);
    }

    @Override
    public int getFill(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FLUID_AMOUNT.get(), 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE :139-153
        if (this.maxFill <= 50) {
            tooltip.add(Component.translatable("desc.item.pipette.corrosive"));
            tooltip.add(Component.translatable("desc.item.pipette.laboratory"));
        } else if (!this.corrosiveFizzles) {
            tooltip.add(Component.translatable("desc.item.pipette.corrosive"));
        } else {
            tooltip.add(Component.translatable("desc.item.pipette.noCorrosive"));
        }
        tooltip.add(Component.literal("Fluid: ").append(getType(stack).getLocalizedName()));
        tooltip.add(Component.literal("Amount: " + getFill(stack) + "/" + getCapacity(stack) + "mB (" + this.maxFill + "mB)"));
    }
}
