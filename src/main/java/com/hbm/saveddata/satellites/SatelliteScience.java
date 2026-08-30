package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.lib.Library;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteScience} (read in full) - the
 * redstone-over-radio "tape drive" data-production satellite, fully self-contained.
 * {@code BobMathUtil.getShortNumber} maps onto this port's own {@link Library#getShortNumber(long)}
 * (identical purpose - a compact SI-suffixed number for a large tick count).
 */
public class SatelliteScience extends Satellite {

    public static final int COOLDOWN = 15 * 60 * 20;
    public long lastScience;

    public static final int SENSOR_DURATION = 100 * 60 * 60 * 20;
    public int sensorProgress;
    public int sensorCount;

    @Override
    public String getType() {
        return "SCIENCE_PROBE";
    }

    @Override
    public boolean hasData(Level level) {
        if (super.hasData(level)) return true;

        if (level.getGameTime() > this.lastScience + COOLDOWN) {
            this.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
            this.lastScience = level.getGameTime();
        }

        return super.hasData(level);
    }

    @Override
    public void onPartDelivered(Level level, ItemStack part) {
        if (!part.isEmpty() && part.getItem() instanceof com.hbm.items.machine.ItemSatellite satItem
                && satItem.getType() == EnumSatType.SCIENCE_SENSOR) {
            this.sensorCount++;
            this.markDirty();
        }
    }

    @Override
    public void onUpdateTick(Level level) {
        if (this.sensorProgress < SENSOR_DURATION) {
            this.sensorProgress += this.sensorCount;
        } else {
            this.sensorProgress = 0;
            this.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
            this.markDirty();
        }
    }

    @Override
    public Component[] getInfo(Level level) {
        int cooldown = (int) ((lastScience + COOLDOWN) - level.getGameTime());
        int seconds = cooldown / 20;

        List<Component> info = new ArrayList<>();
        info.add(Component.translatable("satellite.science.name"));
        info.add(cooldown <= 0 ? Component.translatable("satellite.ready")
                : Component.translatable("satellite.cooldown", (seconds / 60) + "m" + (seconds % 60) + "s"));
        if (this.sensorCount > 0) {
            info.add(Component.translatable("satellite.sensors", this.sensorCount));
            info.add(Component.translatable("satellite.pending", Library.getShortNumber((long) (SENSOR_DURATION - sensorProgress))));
        }
        if (this.driveOutput == EnumDriveType.DISK_ORBITDATA) info.add(Component.translatable("satellite.data"));

        return info.toArray(new Component[0]);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastScience", lastScience);
        nbt.putInt("sensorProgress", sensorProgress);
        nbt.putInt("sensorCount", sensorCount);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastScience = nbt.getLong("lastScience");
        sensorProgress = nbt.getInt("sensorProgress");
        sensorCount = nbt.getInt("sensorCount");
    }

    @Override
    public float[] getColor() {
        return new float[]{1.0F, 1.0F, 0.4F};
    }
}
