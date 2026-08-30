package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Infinite-charge creative-only battery. No mutable state at all - every query answers as if the
 * battery were permanently full, and every mutation is a no-op.
 */
public class ItemBatteryCreative extends Item implements IBatteryItem {

    public ItemBatteryCreative(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return MachineDataComponents.CHARGE;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {}

    @Override
    public void setCharge(ItemStack stack, long i) {}

    @Override
    public void dischargeBattery(ItemStack stack, long i) {}

    @Override
    public long getCharge(ItemStack stack) {
        return Long.MAX_VALUE / 2L;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return Long.MAX_VALUE / 100L;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return Long.MAX_VALUE / 100L;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}
