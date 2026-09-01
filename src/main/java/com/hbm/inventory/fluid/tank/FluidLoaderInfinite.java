package com.hbm.inventory.fluid.tank;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.tool.ItemFluidContainerInfinite;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Random;

/** CE {@code FluidLoaderInfinite}. */
public class FluidLoaderInfinite implements IFluidLoadingHandler {

    private static final Random RAND = new Random();

    @Override
    public boolean fillItem(IItemHandler slots, int in, int out, FluidTankNTM tank) {
        ItemStack inputStack = slots.getStackInSlot(in);
        if (inputStack.isEmpty() || !(inputStack.getItem() instanceof ItemFluidContainerInfinite item)) return false;
        if (!item.allowPressure(tank.getPressure())) return false;
        if (item.getType() != null && tank.getTankType() != item.getType()) return false;
        if (item.getChance() <= 1 || RAND.nextInt(item.getChance()) == 0) {
            tank.setFill(Math.max(tank.getFill() - item.getAmount() * inputStack.getCount(), 0));
        }
        return true;
    }

    @Override
    public boolean emptyItem(IItemHandler slots, int in, int out, FluidTankNTM tank) {
        ItemStack inputStack = slots.getStackInSlot(in);
        if (inputStack.isEmpty() || !(inputStack.getItem() instanceof ItemFluidContainerInfinite item)) {
            return false;
        }
        FluidType itemType = item.getType();
        if (tank.getTankType() == Fluids.NONE) {
            if (itemType != null) {
                if (item.getChance() > 1 && RAND.nextInt(item.getChance()) != 0) return false;
                tank.setTankType(itemType);
            } else return false;
        } else if (item.getChance() > 1 && RAND.nextInt(item.getChance()) != 0) {
            return false;
        }
        if (itemType != null && tank.getTankType() != itemType) return false;
        tank.setFill(Math.min(tank.getFill() + item.getAmount() * inputStack.getCount(), tank.getMaxFill()));
        return true;
    }
}
