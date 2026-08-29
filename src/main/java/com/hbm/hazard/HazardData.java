package com.hbm.hazard;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.type.IHazardType;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable bag of {@link HazardEntry} bound to a single registry key (tag, item or exact stack), plus the
 * override/mutex bits {@link HazardSystem} uses while merging multiple {@link HazardData} sources for one stack.
 */
public class HazardData {

    /**
     * Purges all previously gathered entries when read, useful for when specific items should fully override tag data.
     */
    boolean doesOverride = false;

    /**
     * MUTEX, even more precise than override: makes only specific entries mutually exclusive, for example tag
     * aliases such as {@code c:ingots/plutonium238} and {@code c:ingots/pu238}. Does the opposite of override - if a
     * previous entry already claimed a mutex bit, this one yields instead of replacing it.
     * <p>
     * RESERVED BITS (please keep this up to date)<br>
     * -1: item tags
     */
    int mutexBits = 0b0000_0000_0000_0000_0000_0000_0000_0000;

    public final List<HazardEntry> entries = new ArrayList<>();

    public HazardData addEntry(final IHazardType hazard) {
        return this.addEntry(hazard, 1D, false);
    }

    public HazardData addEntry(final IHazardType hazard, final double level) {
        if (hazard == HazardRegistry.CONTAMINATING && !RadiationConfig.enableContaminationOnGround) return this;
        return this.addEntry(hazard, level, false);
    }

    public HazardData addEntry(final IHazardType hazard, final double level, final boolean override) {
        this.entries.add(new HazardEntry(hazard, level));
        this.doesOverride = override;
        return this;
    }

    public HazardData addEntry(final HazardEntry entry) {
        this.entries.add(entry);
        return this;
    }

    public HazardData setMutex(final int mutex) {
        this.mutexBits = mutex;
        return this;
    }

    public HazardData setOverride(final boolean override) {
        this.doesOverride = override;
        return this;
    }

    public boolean doesOverride() {
        return doesOverride;
    }

    public int getMutex() {
        return mutexBits;
    }
}
