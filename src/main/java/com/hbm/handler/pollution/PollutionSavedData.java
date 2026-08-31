package com.hbm.handler.pollution;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-{@link ServerLevel} backing store for {@link PollutionHandler}, one instance per dimension
 * via {@code level.getDataStorage().computeIfAbsent(factory(), KEY)}.
 * <p>
 * CE/Neo Edition both keep this data in a plain static {@code HashMap<World, PollutionPerWorld>}
 * and hand-roll {@code FileInputStream}/{@code FileOutputStream} + {@code CompressedStreamTools}
 * I/O against a manually-resolved {@code <world>/DIM<n>/data/hbmpollution.dat} path, wired to
 * {@code WorldEvent.Load}/{@code Save}/{@code Unload}. This port instead follows its own
 * already-committed {@code com.hbm.saveddata.TomSaveData}/{@code SatelliteSavedData} precedent
 * (both read in full per {@code docs/phase4/pollution_system.md}'s Key design decisions): a
 * {@link SavedData} subclass rides Minecraft's own per-dimension save/autosave/backup path with no
 * manual file I/O and no {@code LevelEvent.Load}/{@code Unload} handling needed at all -
 * {@code computeIfAbsent} lazily creates-or-loads the instance on first touch (a write call, or the
 * decay tick pass itself). The on-disk *behavior* (one blob per dimension, saved with the world,
 * gone on unload) is unchanged from CE; only the mechanism differs.
 * <p>
 * <b>Callers must call {@link #setDirty()} after every mutation</b> (increment/decrement/set, and
 * once per decay pass) - unlike CE's unconditional whole-file rewrite on every world save,
 * {@link SavedData} only persists when marked dirty. See {@link PollutionHandler} for the actual
 * read/write API and simulation loop; this class is pure storage.
 */
public class PollutionSavedData extends SavedData {

    public static final String KEY = "hbm_pollution";

    /**
     * Keyed by a 64-block coarse cell ({@code new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6)}), NOT
     * a real Minecraft chunk - {@link ChunkPos} is reused purely for its {@code equals}/
     * {@code hashCode}, exactly matching CE's own abuse of the same class (confirmed:
     * {@code PollutionHandler.java} shifts by 6, not 4 - see the research report's Headline finding
     * #2). Decay/diffusion runs over every entry here regardless of whether the corresponding real
     * chunk is currently loaded, matching CE.
     */
    public final Map<ChunkPos, PollutionHandler.PollutionData> pollution = new HashMap<>();

    public PollutionSavedData() {
    }

    public static SavedData.Factory<PollutionSavedData> factory() {
        return new SavedData.Factory<>(PollutionSavedData::new, PollutionSavedData::load);
    }

    /** No caching per level needed - {@code DimensionDataStorage} already does that internally. */
    public static PollutionSavedData forLevel(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), KEY);
    }

    public static PollutionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PollutionSavedData data = new PollutionSavedData();

        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int chunkX = entry.getInt("chunkX");
            int chunkZ = entry.getInt("chunkZ");
            data.pollution.put(new ChunkPos(chunkX, chunkZ), PollutionHandler.PollutionData.fromNBT(entry));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (Map.Entry<ChunkPos, PollutionHandler.PollutionData> entry : pollution.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("chunkX", entry.getKey().x);
            entryTag.putInt("chunkZ", entry.getKey().z);
            entry.getValue().toNBT(entryTag);
            list.add(entryTag);
        }

        tag.put("entries", list);
        return tag;
    }
}
