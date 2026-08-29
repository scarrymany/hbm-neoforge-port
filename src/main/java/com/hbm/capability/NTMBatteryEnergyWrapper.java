package com.hbm.capability;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.config.GeneralConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes any {@link IBatteryItem}-backed {@link ItemStack} as an {@link IEnergyStorage},
 * converting HE (Hbm's Energy) to FE via {@link GeneralConfig#conversionRateHeToRF}.
 *
 * <p>Ported from the {@code Wrapper} inner class of CE's {@code NTMBatteryCapabilityHandler} and
 * promoted to a top-level class: NeoForge's item-capability registration
 * ({@link ModCapabilities#register}) supplies a provider function per item instead of a
 * per-ItemStack {@code AttachCapabilitiesEvent} listener, so the old
 * {@code ICapabilityProvider}/{@code hasCapability}/{@code getCapability} boilerplate this class
 * used to carry is gone - {@code RegisterCapabilitiesEvent.registerItem} does that dispatch
 * itself. The HE&lt;-&gt;FE conversion math is unchanged from CE.
 */
public final class NTMBatteryEnergyWrapper implements IEnergyStorage {

    @NotNull
    private final ItemStack container;
    private final IBatteryItem batteryItem;

    public NTMBatteryEnergyWrapper(@NotNull ItemStack container) {
        this.container = container;
        this.batteryItem = (IBatteryItem) container.getItem();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0 || GeneralConfig.conversionRateHeToRF <= 0) return 0;
        long heBudget = (long) Math.floor(maxReceive / GeneralConfig.conversionRateHeToRF);
        if (heBudget <= 0) return simulate ? 1 : 0;
        long spaceHE = batteryItem.getMaxCharge(container) - batteryItem.getCharge(container);
        long heCanAccept = Math.min(spaceHE, batteryItem.getChargeRate(container));
        long heAccepted = Math.min(heBudget, heCanAccept);
        if (heAccepted > 0 && !simulate) batteryItem.chargeBattery(container, heAccepted);
        long feAccepted = Math.round(heAccepted * GeneralConfig.conversionRateHeToRF);
        return (int) Math.min(maxReceive, Math.min(Integer.MAX_VALUE, feAccepted));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0 || GeneralConfig.conversionRateHeToRF <= 0) return 0;
        long heBudget = (long) Math.floor(maxExtract / GeneralConfig.conversionRateHeToRF);
        if (heBudget <= 0) return simulate ? 1 : 0;
        long heAvailable = Math.min(batteryItem.getCharge(container), batteryItem.getDischargeRate(container));
        long heExtracted = Math.min(heBudget, heAvailable);
        if (heExtracted > 0 && !simulate) batteryItem.dischargeBattery(container, heExtracted);
        long feExtracted = Math.round(heExtracted * GeneralConfig.conversionRateHeToRF);
        return (int) Math.min(maxExtract, Math.min(Integer.MAX_VALUE, feExtracted));
    }

    @Override
    public int getEnergyStored() {
        return GeneralConfig.conversionRateHeToRF <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE,
                Math.round(batteryItem.getCharge(container) * GeneralConfig.conversionRateHeToRF));
    }

    @Override
    public int getMaxEnergyStored() {
        return GeneralConfig.conversionRateHeToRF <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE,
                Math.round(batteryItem.getMaxCharge(container) * GeneralConfig.conversionRateHeToRF));
    }

    @Override
    public boolean canExtract() {
        return batteryItem.getDischargeRate(container) > 0 && batteryItem.getCharge(container) > 0;
    }

    @Override
    public boolean canReceive() {
        return batteryItem.getChargeRate(container) > 0 && batteryItem.getCharge(container) < batteryItem.getMaxCharge(container);
    }
}
