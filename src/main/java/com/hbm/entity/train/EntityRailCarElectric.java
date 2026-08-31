package com.hbm.entity.train;

import com.hbm.api.energymk2.IBatteryItem;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.train.EntityRailCarElectric} (72 lines) - adds a synced
 * {@link #POWER} buffer (HE, not vanilla FE/RF - matching this port's own {@link IBatteryItem}
 * convention) charged from an {@link IBatteryItem}-implementing item in a subclass-chosen slot.
 * <p>
 * <b>Not ported</b>: CE's {@code ModItems.battery_creative} instant-max-charge branch - this port has
 * no creative-battery item yet (confirmed by a fresh grep of {@code ModItems.java}; not part of any
 * landed phase so far). The real {@link IBatteryItem} discharge path (CE's primary, non-creative
 * charging mechanism) is fully ported below and is not gated on that missing item at all - see this
 * package's {@code knownGaps} for the one-line follow-up once a creative battery item exists.
 */
public abstract class EntityRailCarElectric extends EntityRailCarRidable {

    protected static final EntityDataAccessor<Integer> POWER =
            SynchedEntityData.defineId(EntityRailCarElectric.class, EntityDataSerializers.INT);

    protected EntityRailCarElectric(EntityType<? extends EntityRailCarElectric> type, Level level) {
        super(type, level);
    }

    public abstract int getMaxPower();
    public abstract int getPowerConsumption();

    public boolean hasChargeSlot() { return false; }
    public int getChargeSlot() { return 0; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0);
    }

    @Override public boolean canAccelerate() { return true; }
    @Override public void consumeFuel() { }

    public void setPower(int power) {
        this.entityData.set(POWER, power);
    }

    public int getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {

            if (this.hasChargeSlot()) {
                ItemStack stack = this.getItem(this.getChargeSlot());

                if (!stack.isEmpty() && stack.getItem() instanceof IBatteryItem battery) {
                    int powerNeeded = this.getMaxPower() - this.getPower();
                    long powerProvided = Math.min(battery.getDischargeRate(stack), battery.getCharge(stack));
                    int powerTransfered = (int) Math.min(powerNeeded, powerProvided);

                    if (powerTransfered > 0) {
                        battery.dischargeBattery(stack, powerTransfered);
                        this.setPower(this.getPower() + powerTransfered);
                    }
                }
            }
        }
    }
}
