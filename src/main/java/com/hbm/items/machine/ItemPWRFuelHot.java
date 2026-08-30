package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;

/**
 * Spent/hot PWR fuel byproduct marker, ported from CE's {@code ModItems.pwr_fuel_hot}
 * ({@code new ItemEnumMulti<>("pwr_fuel_hot", EnumPWRFuel.VALUES, true, "pwr_fuel_hot")} - a plain
 * metadata-variant marker with zero logic of its own, confirmed by reading CE's
 * {@code ModItems.java:1206}). {@link com.hbm.blockentity.machine.PWRControllerBlockEntity} deposits
 * this into its slot 1 once a loaded {@link EnumPWRFuel} grade finishes its process cycle
 * ({@code TileEntityPWRController.update()}'s {@code inventory.setStackInSlot(1, new
 * ItemStack(ModItems.pwr_fuel_hot, 1, typeLoaded))} branch).
 *
 * <p>Not registered by Phase 1's {@code MachineItems} (which only ported the fresh
 * {@link ItemPWRFuel} instances, see that class's own "CE also registers pwr_fuel_hot/..." comment)
 * - registered instead by {@link PWRHotFuelItems}, this package's own new registration class, so this
 * PWR package does not need to edit {@code MachineItems.java}/{@code ModItems.java} to add the 15
 * variants it needs. Each (multiplicity-free, single registry-per-grade) instance mirrors this port's
 * established metadata-flattening convention (compare {@link ItemPWRFuel} itself).
 */
public class ItemPWRFuelHot extends ItemBase {

    private final EnumPWRFuel type;

    public ItemPWRFuelHot(EnumPWRFuel type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EnumPWRFuel getType() {
        return this.type;
    }
}
