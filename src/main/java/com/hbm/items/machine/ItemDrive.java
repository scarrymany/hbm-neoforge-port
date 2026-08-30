package com.hbm.items.machine;

/**
 * Not an item - a bare enum namespace CE used to share drive-content identity between whatever
 * class actually implements the satellite/data-drive item (in {@code items.special}, per the
 * items_machine research report) and its consumers. Ported verbatim; the real drive item class,
 * when it lands in {@code items.special}, should reference {@link EnumDriveType} from here rather
 * than duplicating it.
 */
public class ItemDrive {

    private ItemDrive() {}

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
