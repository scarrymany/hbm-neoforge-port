package com.hbm.saveddata.satellites;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.SatelliteSavedData} (91 lines, read in full)
 * onto this port's already-proven {@link SavedData} pattern - see
 * {@code com.hbm.saveddata.TomSaveData}, read in full as the model this class follows exactly
 * ({@link SavedData.Factory}, {@code ServerLevel#getDataStorage().computeIfAbsent(factory, key)} for
 * the lazy-create-once-per-world lookup CE's own {@code getData(World)} performed by hand).
 * <p>
 * CE's {@code Int2ObjectOpenHashMap<Satellite>} becomes a plain {@link HashMap} (this port has no
 * fastutil dependency pulled in for this package) serialized as an indexed flat NBT list, matching
 * CE's own {@code writeToNBT}/{@code readFromNBT} shape exactly (no {@code Codec}-based map
 * serialization needed - a manual indexed-loop read/write is the direct translation).
 */
public class SatelliteSavedData extends SavedData {

    public static final String KEY = "satellites";

    public final Map<Integer, Satellite> sats = new HashMap<>();

    public static SavedData.Factory<SatelliteSavedData> factory() {
        return new SavedData.Factory<>(SatelliteSavedData::new, SatelliteSavedData::load);
    }

    public SatelliteSavedData() {
    }

    public boolean isFreqTaken(int freq) {
        return getSatFromFreq(freq) != null;
    }

    public Satellite getSatFromFreq(int freq) {
        return sats.get(freq);
    }

    /** Convenience overload matching CE's own {@code getData(World)} call sites. */
    public static SatelliteSavedData getData(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("SatelliteSavedData.getData() called on a client Level - satellite state is server-authoritative only.");
        }
        return serverLevel.getDataStorage().computeIfAbsent(factory(), KEY);
    }

    public static SatelliteSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SatelliteSavedData data = new SatelliteSavedData();

        int satCount = tag.getInt("satCount");
        for (int i = 0; i < satCount; i++) {
            Satellite sat = Satellite.create(tag.getInt("sat_id_" + i));
            if (sat == null) continue;

            sat.readFromNBT(tag.getCompound("sat_data_" + i));
            int freq = tag.getInt("sat_freq_" + i);
            data.sats.put(freq, sat);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("satCount", sats.size());

        int i = 0;
        for (Map.Entry<Integer, Satellite> entry : sats.entrySet()) {
            CompoundTag data = new CompoundTag();
            entry.getValue().writeToNBT(data);

            tag.putInt("sat_id_" + i, entry.getValue().getID());
            tag.put("sat_data_" + i, data);
            tag.putInt("sat_freq_" + i, entry.getKey());
            i++;
        }

        return tag;
    }
}
