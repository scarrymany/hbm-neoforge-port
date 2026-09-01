package com.hbm.items.special;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of CE's {@code ItemCell} ({@code hbm:cell}). CE keyed the held fluid off item metadata
 * (0 = empty, otherwise an NTM fluid id); post-1.13 has no item metadata, and per the explicit
 * design decision recorded in docs/phase1/items_special.md's per-file table, this becomes a single
 * {@code hbm:cell} item carrying the fluid id as a {@link SpecialItemComponents#CELL_FLUID_ID} data
 * component instead of one flattened item per fluid (which would create dozens/hundreds of registry
 * entries for what is conceptually one container type).
 * <p>
 * Not ported: CE's {@code onEntityItemUpdate} explosion when an antimatter ({@code Fluids.AMAT}) or
 * anti-schrabidium ({@code Fluids.ASCHRAB}) cell is dropped and burns/lands - both effects spawn
 * nuke-explosion entity classes that don't exist in the port yet (no entity system ported through
 * Phase 1, see docs/phase1/items_special.md finding 4's sibling systems). The tooltip warning for
 * both fluids is kept, since it carries no such dependency.
 */
public class ItemCell extends Item implements IFillableItem {

    /** CE {@code EnumCell} whitelist — 1000 mB, all-or-nothing like FluidContainerRegistry. */
    public static final int CAPACITY = 1000;

    public ItemCell(Properties properties) {
        super(properties);
    }

    /** Runtime identity — Fluids fields are assigned in {@code Fluids.register()}, not class-init. */
    private static boolean allowed(FluidType type) {
        return type == Fluids.UF6 || type == Fluids.PUF6 || type == Fluids.AMAT
                || type == Fluids.DEUTERIUM || type == Fluids.TRITIUM
                || type == Fluids.SAS3 || type == Fluids.ASCHRAB;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return isEmptyCell(stack) && allowed(type);
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack) || amount < CAPACITY) return amount;
        stack.set(SpecialItemComponents.CELL_FLUID_ID.get(), type.getID());
        return amount - CAPACITY;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return type != null && getFluidType(stack) == type;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (!providesFluid(type, stack) || amount < CAPACITY) return 0;
        stack.remove(SpecialItemComponents.CELL_FLUID_ID.get());
        return CAPACITY;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return getFluidType(stack);
    }

    @Override
    public int getFill(ItemStack stack) {
        return isEmptyCell(stack) ? 0 : CAPACITY;
    }

    public static boolean isEmptyCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemCell && getFluidId(stack) == 0;
    }

    public static boolean isFullCell(ItemStack stack, FluidType type) {
        return getFluidType(stack) == type;
    }

    public static boolean hasFluid(ItemStack stack, FluidType type) {
        return getFluidType(stack) == type;
    }

    @Nullable
    public static FluidType getFluidType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemCell) || getFluidId(stack) == 0) {
            return null;
        }
        return Fluids.fromID(getFluidId(stack));
    }

    public static int getFluidId(ItemStack stack) {
        return stack.getOrDefault(SpecialItemComponents.CELL_FLUID_ID.get(), 0);
    }

    public static ItemStack getFullCell(Item cellItem, FluidType fluid, int amount) {
        ItemStack stack = new ItemStack(cellItem, amount);
        stack.set(SpecialItemComponents.CELL_FLUID_ID.get(), fluid.getID());
        return stack;
    }

    public static ItemStack getFullCell(Item cellItem, FluidType fluid) {
        return getFullCell(cellItem, fluid, 1);
    }

    /**
     * CE's {@code ItemCell.hasEmptyCell(EntityPlayer)}/{@code consumeEmptyCell(EntityPlayer)}
     * (confirmed present in CE, absent from this port's {@link ItemCell} until this addition - see
     * {@code docs/phase3/scattered_military_items.md}'s "Key design/API decisions": a
     * player-inventory scan over the existing {@link #isEmptyCell(ItemStack)} primitive, added here
     * so future items needing "does the player have a spare cell" share one implementation rather
     * than each duplicating the scan). Scans main inventory, armor and offhand, in that order -
     * matching CE's real {@code InventoryPlayer#getSizeInventory()} flat-index scan order (main
     * slots before armor/offhand).
     */
    public static boolean hasEmptyCell(Player player) {
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (isEmptyCell(stack)) return true;
        }
        for (ItemStack stack : inv.armor) {
            if (isEmptyCell(stack)) return true;
        }
        for (ItemStack stack : inv.offhand) {
            if (isEmptyCell(stack)) return true;
        }
        return false;
    }

    /** See {@link #hasEmptyCell(Player)}. Shrinks the first empty cell found by one; no-op if none exist. */
    public static void consumeEmptyCell(Player player) {
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (isEmptyCell(stack)) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack stack : inv.armor) {
            if (isEmptyCell(stack)) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack stack : inv.offhand) {
            if (isEmptyCell(stack)) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return !isEmptyCell(stack);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return hasCraftingRemainingItem(stack) ? new ItemStack(this) : ItemStack.EMPTY;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // Dangerous-drop explosion deferred - see class javadoc.
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isEmptyCell(stack)) {
            return Component.translatable("item.hbm.cell_empty");
        }
        FluidType type = getFluidType(stack);
        if (type != null) {
            return Component.translatable("item.hbm.cell_full", type.getLocalizedName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidType type = getFluidType(stack);
        if (type == Fluids.AMAT) {
            tooltip.add(Component.literal("Exposure to matter will lead to violent annihilation!").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("[Dangerous Drop]").withStyle(ChatFormatting.RED));
        } else if (type == Fluids.ASCHRAB) {
            tooltip.add(Component.literal("Exposure to matter will create a folkvangr field!").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("[Dangerous Drop]").withStyle(ChatFormatting.RED));
        }
    }
}
