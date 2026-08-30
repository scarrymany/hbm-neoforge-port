package com.hbm.items.machine;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Classic reactor fuel rod: a life-counter NBT item with no reference to the reactor tile entity
 * that eventually burns it (Phase 2). Base class for per-instance subclassing exactly as CE had
 * it (e.g. {@link ItemPlateFuel}).
 */
public class ItemFuelRod extends Item {

    protected final int lifeTime;

    public ItemFuelRod(int lifeTime, Properties properties) {
        super(properties.durability(0).setNoRepair());
        this.lifeTime = lifeTime;
    }

    public int getLifeTime() {
        return this.lifeTime;
    }

    public static void setLifeTime(ItemStack stack, int time) {
        stack.set(MachineDataComponents.FUEL_ROD_LIFE.get(), time);
    }

    public static int getLifeTime(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.FUEL_ROD_LIFE.get(), 0);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLifeTime(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemFuelRod rod) || rod.lifeTime <= 0) return 0;
        return Math.round(13.0F * getLifeTime(stack) / (float) rod.lifeTime);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFAA00;
    }
}
