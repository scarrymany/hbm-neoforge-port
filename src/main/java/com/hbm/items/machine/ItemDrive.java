package com.hbm.items.machine;

import net.minecraft.world.item.Item;

import java.util.Locale;

/**
 * CE {@code ItemDrive} / {@code EnumDriveType}. One registry id per type
 * ({@code drive_flash_empty} … {@code drive_klaus}); display name stays the CE
 * {@code item.hbm.drive.<type>} lang key.
 */
public class ItemDrive extends Item {

    private final EnumDriveType type;

    public ItemDrive(EnumDriveType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EnumDriveType getDriveType() {
        return type;
    }

    @Override
    public String getDescriptionId() {
        return "item.hbm.drive." + type.name().toLowerCase(Locale.ROOT);
    }

    public enum EnumDriveType {
        FLASH_EMPTY,
        DISK_EMPTY,
        FLASH_BROKEN,
        DISK_BROKEN,

        FLASH_FLIGHTSIM,            // precalc for spaceflight
        FLASH_PARTICLESIM,          // precalc for fusion

        DISK_FLIGHTDATA,            // raw data from satellite
        DISK_FLIGHTDATA_PROCESSED,  // processed data from satellite
        DISK_ORBITDATA,             // raw sensor relay data
        DISK_ORBITDATA_PROCESSED,   // processed data from sensor relay

        KLAUS;                      // kkklanker

        public static final EnumDriveType[] VALUES = values();
    }
}
