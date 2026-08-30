package com.hbm.saveddata;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Ported from CE's {@code TomSaveData}: a small per-world save file tracking the global
 * nuclear-winter/impact simulation state ({@code dust}, {@code fire}, {@code impact}, plus a
 * reserved space-addon coordinate/time block). Used by {@link com.hbm.blocks.generic.BlockDirt}
 * to decide whether scorched dirt is allowed to regrow grass yet. This is a small self-contained
 * data class, not a machine/tile-entity dependency, so it is ported alongside {@code BlockDirt}
 * rather than deferred to a later phase.
 * <p>
 * CE's {@code lastCachedUnsafe} escape hatch (for biome-generation call sites with no world
 * instance available) is preserved verbatim via {@link #getLastCachedOrNull()}.
 */
public class TomSaveData extends SavedData {

    public static final String KEY = "impactData";

    public float dust;
    public float fire;
    public boolean impact;

    // reserved for space-addon usage, ported verbatim from CE
    public long time;
    public long dtime;
    public int x;
    public int z;

    private static TomSaveData lastCachedUnsafe = null;

    public static SavedData.Factory<TomSaveData> factory() {
        return new SavedData.Factory<>(TomSaveData::new, TomSaveData::load);
    }

    public TomSaveData() {
    }

    /** No caching per world needed - Minecraft's save structure already does that; call as much as wanted. */
    public static TomSaveData forWorld(ServerLevel level) {
        TomSaveData result = level.getDataStorage().computeIfAbsent(factory(), KEY);
        lastCachedUnsafe = result;
        return result;
    }

    /**
     * Certain biome events do not have access to a world instance; in those cases we have to rely
     * on a possibly incorrect cached result. However, since world-gen invokes {@link #forWorld}
     * quite a lot, it is safe to say that in most cases this ends up with the correct result.
     */
    public static TomSaveData getLastCachedOrNull() {
        return lastCachedUnsafe;
    }

    public static void resetLastCached() {
        lastCachedUnsafe = null;
    }

    public static TomSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        TomSaveData data = new TomSaveData();
        data.dust = tag.getFloat("dust");
        data.fire = tag.getFloat("fire");
        data.impact = tag.getBoolean("impact");
        data.time = tag.getLong("time");
        data.dtime = tag.getLong("dtime");
        data.x = tag.getInt("x");
        data.z = tag.getInt("z");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("dust", dust);
        tag.putFloat("fire", fire);
        tag.putBoolean("impact", impact);
        tag.putLong("time", time);
        tag.putLong("dtime", dtime);
        tag.putInt("x", x);
        tag.putInt("z", z);
        return tag;
    }
}
