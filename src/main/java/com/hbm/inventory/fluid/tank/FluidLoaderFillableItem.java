package com.hbm.inventory.fluid.tank;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** CE {@code FluidLoaderFillableItem} — IFillableItem + armor-mod path. */
public class FluidLoaderFillableItem implements IFluidLoadingHandler {

    @Override
    public boolean fillItem(IItemHandler slots, int in, int out, FluidTankNTM tank) {
        if (tank.getPressure() != 0) return false;
        ItemStack inputStack = slots.getStackInSlot(in);
        if (inputStack.isEmpty()) return true;
        if (inputStack.getCount() > 1) {
            ItemStack singleItem = slots.extractItem(in, 1, true);
            int moved = fill(singleItem, tank);
            if (moved > 0 && slots.insertItem(out, singleItem, true).isEmpty()) {
                slots.extractItem(in, 1, false);
                tank.setFill(tank.getFill() - moved);
                slots.insertItem(out, singleItem, false);
                return true;
            }
            return false;
        }
        ItemStack singleItem = slots.extractItem(in, 1, true);
        if (singleItem.isEmpty()) return true;
        int movedSim = fill(singleItem, tank);
        if (movedSim <= 0) return false;
        boolean wouldEmptyTank = (tank.getFill() - movedSim) == 0;
        boolean preferOut = isItemFull(singleItem, tank.getTankType()) || wouldEmptyTank;
        boolean toOut = preferOut && slots.insertItem(out, singleItem, true).isEmpty();
        slots.extractItem(in, 1, false);
        tank.setFill(tank.getFill() - movedSim);
        slots.insertItem(toOut ? out : in, singleItem, false);
        return true;
    }

    private static int fill(ItemStack stack, FluidTankNTM tank) {
        if (tank.getFill() <= 0) return 0;
        int movedTotal = 0;
        FluidType type = tank.getTankType();
        int remaining = tank.getFill();
        if (remaining > 0 && stack.getItem() instanceof ArmorItem && ArmorModHandler.hasMods(stack)) {
            ItemStack[] mods = ArmorModHandler.pryMods(stack);
            for (ItemStack mod : mods) {
                if (remaining <= 0) break;
                if (mod != null && !mod.isEmpty() && mod.getItem() instanceof IFillableItem fillableMod) {
                    if (fillableMod.acceptsFluid(type, mod)) {
                        int remainder = fillableMod.tryFill(type, remaining, mod);
                        int moved = remaining - remainder;
                        if (moved > 0) {
                            movedTotal += moved;
                            remaining -= moved;
                            ArmorModHandler.applyMod(stack, mod);
                        }
                    }
                }
            }
        }
        if (remaining > 0 && stack.getItem() instanceof IFillableItem fillable) {
            if (fillable.acceptsFluid(type, stack)) {
                int remainder = fillable.tryFill(type, remaining, stack);
                int moved = remaining - remainder;
                if (moved > 0) movedTotal += moved;
            }
        }
        return movedTotal;
    }

    @Override
    public boolean emptyItem(IItemHandler slots, int in, int out, FluidTankNTM tank) {
        ItemStack inputStack = slots.getStackInSlot(in);
        if (inputStack.isEmpty()) return true;
        if (inputStack.getCount() > 1) {
            ItemStack singleItem = slots.extractItem(in, 1, true);
            FluidType chosen = tank.getTankType() != Fluids.NONE ? tank.getTankType() : pickEmptyingType(singleItem);
            int moved = empty(singleItem, tank);
            if (moved > 0 && slots.insertItem(out, singleItem, true).isEmpty()) {
                if (tank.getTankType() == Fluids.NONE && chosen != null && chosen != Fluids.NONE) {
                    tank.setTankType(chosen);
                }
                slots.extractItem(in, 1, false);
                tank.setFill(tank.getFill() + moved);
                slots.insertItem(out, singleItem, false);
                return true;
            }
            return false;
        }
        ItemStack singleItem = slots.extractItem(in, 1, true);
        if (singleItem.isEmpty()) return true;
        FluidType chosen = tank.getTankType() == Fluids.NONE ? pickEmptyingType(singleItem) : tank.getTankType();
        int movedSim = empty(singleItem, tank);
        if (movedSim <= 0) return false;
        boolean wouldFillTank = (tank.getFill() + movedSim) >= tank.getMaxFill();
        boolean preferOut = isItemEmpty(singleItem) || wouldFillTank;
        boolean toOut = preferOut && slots.insertItem(out, singleItem, true).isEmpty();
        if (tank.getTankType() == Fluids.NONE && chosen != null && chosen != Fluids.NONE) {
            tank.setTankType(chosen);
        }
        slots.extractItem(in, 1, false);
        tank.setFill(tank.getFill() + movedSim);
        slots.insertItem(toOut ? out : in, singleItem, false);
        return true;
    }

    private static int empty(ItemStack stack, FluidTankNTM tank) {
        int space = tank.getMaxFill() - tank.getFill();
        if (space <= 0) return 0;
        int movedTotal = 0;
        FluidType tankType = tank.getTankType();
        FluidType targetType = tankType != Fluids.NONE ? tankType : pickEmptyingType(stack);
        if (targetType == null || targetType == Fluids.NONE) return 0;
        if (stack.getItem() instanceof ArmorItem && ArmorModHandler.hasMods(stack)) {
            ItemStack[] mods = ArmorModHandler.pryMods(stack);
            for (ItemStack mod : mods) {
                if (movedTotal >= space) break;
                if (mod != null && !mod.isEmpty() && mod.getItem() instanceof IFillableItem fillableMod) {
                    if (fillableMod.providesFluid(targetType, mod)) {
                        int moved = fillableMod.tryEmpty(targetType, space - movedTotal, mod);
                        if (moved > 0) {
                            movedTotal += moved;
                            ArmorModHandler.applyMod(stack, mod);
                        }
                    }
                }
            }
        }
        if (movedTotal < space && stack.getItem() instanceof IFillableItem fillable) {
            if (fillable.providesFluid(targetType, stack)) {
                int moved = fillable.tryEmpty(targetType, space - movedTotal, stack);
                if (moved > 0) movedTotal += moved;
            }
        }
        return movedTotal;
    }

    private static FluidType pickEmptyingType(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem && ArmorModHandler.hasMods(stack)) {
            ItemStack[] mods = ArmorModHandler.pryMods(stack.copy());
            for (ItemStack mod : mods) {
                if (mod != null && !mod.isEmpty() && mod.getItem() instanceof IFillableItem fillableMod) {
                    FluidType t = fillableMod.getFirstFluidType(mod);
                    if (t != null && t != Fluids.NONE) return t;
                }
            }
        }
        if (stack.getItem() instanceof IFillableItem fillable) {
            FluidType t = fillable.getFirstFluidType(stack);
            if (t != null && t != Fluids.NONE) return t;
        }
        return Fluids.NONE;
    }

    private static boolean isItemFull(ItemStack stack, FluidType type) {
        if (stack.getItem() instanceof IFillableItem fillable && fillable.acceptsFluid(type, stack)) {
            return fillable.tryFill(type, 1, stack.copy()) >= 1;
        }
        return true;
    }

    private static boolean isItemEmpty(ItemStack stack) {
        if (stack.getItem() instanceof IFillableItem fillable) {
            FluidType itemFluid = fillable.getFirstFluidType(stack);
            if (itemFluid != null && itemFluid != Fluids.NONE) {
                return fillable.tryEmpty(itemFluid, 1, stack.copy()) <= 0;
            }
        }
        return true;
    }
}
