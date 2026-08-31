package com.hbm.handler;

import com.hbm.interfaces.IClimbable;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.handler.ClimbableRegistry} (451 lines, read in full): a
 * chunk/section-bucketed spatial index of every registered {@link IClimbable} so entity movement
 * code can cheaply ask "is this entity touching a climbable right now" without scanning every
 * climbable in the world. Consumed by {@code com.hbm.blockentity.machine.MachineCrystallizerBlockEntity}
 * (CE: {@code TileEntityMachineCrystallizer}), whose multiblock tower has a ladder-shaped climb box
 * running up one side.
 * <p>
 * Two adaptations from CE's 1.12.2 shape, both purely mechanical (same algorithm, same bucket/
 * promotion behavior):
 * <ul>
 *   <li>CE keyed the per-dimension map by {@code World.provider.getDimension()}'s {@code int}; modern
 *   Minecraft has no integer dimension id, so this port keys by {@link ResourceKey}{@code <Level>}
 *   (via {@link Level#dimension()}) in a fastutil {@link Object2ObjectOpenHashMap} instead of
 *   fastutil's {@code Int2ObjectMap}.</li>
 *   <li>CE's {@code SectionBucket} packed a subchunk-Y into its already-packed chunk-key long via its
 *   own {@code Library.sectionToLong(long chunkKey, int subY)}. This port's {@link Library} carries a
 *   different (but equally self-consistent, see its own javadoc) section-key bit layout keyed off
 *   raw {@code (sectionX, sectionZ, sectionY)} ints
 *   ({@link Library#sectionToLong(int, int, int)}), so each bucket here keeps its owning chunk's
 *   {@code cx}/{@code cz} as plain ints instead of CE's single packed {@code chunkKey} field, and
 *   builds section keys straight from those - same effect, no decode step needed anywhere.</li>
 * </ul>
 * Everything else - the flat-list-until-{@link #SECTION_PROMOTION_THRESHOLD}-then-promote-to-
 * per-section-buckets strategy, the client/server-side split, the reverse index for O(1) unregister,
 * the {@code +-1.0e-6} query inflation - matches CE exactly.
 * <p>
 * <b>Not ported</b>: CE's other real consumer of this registry, {@code ModEventHandler.onCheckLadder}
 * (a {@code @SubscribeEvent} on Forge's {@code CheckLadderEvent} that calls
 * {@link #isEntityOnAny(Level, LivingEntity)} to make an entity actually treat itself as climbing
 * when it overlaps a registered climbable). {@code CheckLadderEvent} is a Forge-only event with no
 * confirmed NeoForge 1.21.1 equivalent (absent from both this port's own event handlers and Neo
 * Edition) - {@link #isEntityOnAny(Level, LivingEntity)} and {@link #getClimbablesInAABB(Level, AABB)}
 * are ported and correct, but nothing calls them yet, so a registered {@link IClimbable} is indexed
 * and queryable without yet making anything actually climb it. Whoever finds or builds this port's
 * real hook point (a mixin into {@code LivingEntity#onClimbable()} is the most likely candidate) wires
 * it here. {@link #onLevelUnload} below (CE: {@code ModEventHandler.worldUnload}) is ported, since
 * {@code LevelEvent.Unload} is a confirmed real hook already used elsewhere in this port (see
 * {@code RadiationSystemNT.onLevelUnload}).
 */
@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class ClimbableRegistry {
    private static final int SECTION_PROMOTION_THRESHOLD = 8;
    private static final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<Bucket>> CLIENT_BY_DIM = new Object2ObjectOpenHashMap<>();
    private static final IdentityHashMap<IClimbable, Entry> CLIENT_REVERSE = new IdentityHashMap<>();
    private static final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<Bucket>> SERVER_BY_DIM = new Object2ObjectOpenHashMap<>();
    private static final IdentityHashMap<IClimbable, Entry> SERVER_REVERSE = new IdentityHashMap<>();

    private ClimbableRegistry() {
    }

    private static Object2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<Bucket>> byDim(Level w) {
        return w.isClientSide ? CLIENT_BY_DIM : SERVER_BY_DIM;
    }

    private static IdentityHashMap<IClimbable, Entry> reverse(Level w) {
        return w.isClientSide ? CLIENT_REVERSE : SERVER_REVERSE;
    }

    /**
     * Register a climbable across all chunks overlapped by its climb AABB
     */
    public static void register(IClimbable c) {
        Level w = c.world();
        ResourceKey<Level> dim = w.dimension();

        IdentityHashMap<IClimbable, Entry> rev = reverse(w);
        if (rev.containsKey(c)) {
            unregister(c);
        }

        AABB aabb = c.getClimbAABBForIndexing();
        Entry entry = new Entry(dim);

        if (aabb == null) {
            int cx = c.pos().getX() >> 4;
            int cz = c.pos().getZ() >> 4;
            long key = ChunkPos.asLong(cx, cz);
            addToChunk(w, dim, key, cx, cz, c);
            entry.keys.add(key);
        } else {
            int minCX = Mth.floor(aabb.minX) >> 4;
            int maxCX = Mth.floor(aabb.maxX) >> 4;
            int minCZ = Mth.floor(aabb.minZ) >> 4;
            int maxCZ = Mth.floor(aabb.maxZ) >> 4;

            for (int cx = minCX; cx <= maxCX; cx++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    long key = ChunkPos.asLong(cx, cz);
                    addToChunk(w, dim, key, cx, cz, c);
                    entry.keys.add(key);
                }
            }
        }

        rev.put(c, entry);
    }

    /**
     * Remove a climbable from every chunk it was registered to (safe if not present).
     */
    public static void unregister(IClimbable c) {
        Level w = c.world();
        IdentityHashMap<IClimbable, Entry> rev = reverse(w);
        Entry e = rev.remove(c);
        if (e == null) return;

        Long2ObjectOpenHashMap<Bucket> byChunk = byDim(w).get(e.dim);
        if (byChunk == null) return;

        for (long key : e.keys) {
            Bucket bucket = byChunk.get(key);
            if (bucket == null) continue;
            bucket.remove(c);
            if (bucket.isEmpty()) {
                byChunk.remove(key);
            }
        }

        if (byChunk.isEmpty()) {
            byDim(w).remove(e.dim);
        }
    }

    /**
     * If a climbable's AABB or anchor changed, call this to rebuild its index.
     */
    public static void refresh(IClimbable c) {
        unregister(c);
        register(c);
    }

    /**
     * Hot-path query: is the entity intersecting any climbable in nearby chunks?
     */
    public static boolean isEntityOnAny(Level w, LivingEntity e) {
        ResourceKey<Level> dim = w.dimension();
        Long2ObjectOpenHashMap<Bucket> byChunk = byDim(w).get(dim);
        if (byChunk == null || byChunk.isEmpty()) return false;

        AABB bb = e.getBoundingBox();

        int minCX = Mth.floor(bb.minX) >> 4;
        int maxCX = Mth.floor(bb.maxX) >> 4;
        int minSY = minSectionCoord(bb);
        int maxSY = maxSectionCoord(bb);
        int minCZ = Mth.floor(bb.minZ) >> 4;
        int maxCZ = Mth.floor(bb.maxZ) >> 4;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                Bucket bucket = byChunk.get(key);
                if (bucket == null || bucket.isEmpty()) continue;
                if (bucket.isEntityOnAny(w, e, minSY, maxSY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clear all climbables for a dimension on the given side
     */
    public static void clearDimension(Level w) {
        ResourceKey<Level> dim = w.dimension();
        byDim(w).remove(dim);
        reverse(w).values().removeIf(entry -> entry.dim.equals(dim));
    }

    /**
     * CE: {@code ModEventHandler.worldUnload(WorldEvent.Unload)}, unguarded by side (Forge's
     * {@code WorldEvent.Unload} fires for a client-side world unload, e.g. on disconnect, too, and CE
     * calls {@code ClimbableRegistry.clearDimension(e.getWorld())} regardless) - matched here by not
     * restricting to {@link net.minecraft.server.level.ServerLevel}. Default GAME bus is correct:
     * {@code LevelEvent.Unload} is not an {@code IModBusEvent}.
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            clearDimension(level);
        }
    }

    /**
     * Clear everything on both sides.
     */
    public static void clearAll() {
        CLIENT_BY_DIM.clear();
        CLIENT_REVERSE.clear();
        SERVER_BY_DIM.clear();
        SERVER_REVERSE.clear();
    }

    /**
     * Count climbables registered in a dimension on the given side.
     */
    public static int countClimbablesInDim(Level w, ResourceKey<Level> dim) {
        int total = 0;
        for (Entry entry : reverse(w).values()) {
            if (entry.dim.equals(dim)) {
                total++;
            }
        }
        return total;
    }

    public static List<IClimbable> getClimbablesInAABB(Level w, @Nullable AABB aabb) {
        ArrayList<IClimbable> out = new ArrayList<>();
        if (aabb == null) return out;

        ResourceKey<Level> dim = w.dimension();
        Long2ObjectOpenHashMap<Bucket> byChunk = byDim(w).get(dim);
        if (byChunk == null) return out;
        ReferenceOpenHashSet<IClimbable> seen = new ReferenceOpenHashSet<>();
        AABB q = aabb.inflate(1.0e-6);

        int minCX = Mth.floor(aabb.minX) >> 4;
        int maxCX = Mth.floor(aabb.maxX) >> 4;
        int minSY = minSectionCoord(aabb);
        int maxSY = maxSectionCoord(aabb);
        int minCZ = Mth.floor(aabb.minZ) >> 4;
        int maxCZ = Mth.floor(aabb.maxZ) >> 4;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                Bucket bucket = byChunk.get(key);
                if (bucket == null || bucket.isEmpty()) continue;
                bucket.collectIntersecting(w, q, minSY, maxSY, seen, out);
            }
        }
        return out;
    }

    private static void addToChunk(Level w, ResourceKey<Level> dim, long key, int cx, int cz, IClimbable c) {
        Object2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<Bucket>> side = byDim(w);
        Long2ObjectOpenHashMap<Bucket> byChunk = side.get(dim);
        if (byChunk == null) {
            byChunk = new Long2ObjectOpenHashMap<>();
            side.put(dim, byChunk);
        }
        Bucket bucket = byChunk.get(key);
        if (bucket == null) {
            bucket = new FlatBucket(cx, cz);
            byChunk.put(key, bucket);
        }
        Bucket next = bucket.add(c);
        if (next != bucket) {
            byChunk.put(key, next);
        }
    }

    private static int minSectionCoord(AABB aabb) {
        return Mth.floor(aabb.minY) >> 4;
    }

    private static int maxSectionCoord(AABB aabb) {
        return Mth.floor(aabb.maxY) >> 4;
    }

    private static int minSectionCoord(IClimbable c) {
        AABB aabb = c.getClimbAABBForIndexing();
        return aabb != null ? minSectionCoord(aabb) : c.pos().getY() >> 4;
    }

    private static int maxSectionCoord(IClimbable c) {
        AABB aabb = c.getClimbAABBForIndexing();
        return aabb != null ? maxSectionCoord(aabb) : c.pos().getY() >> 4;
    }

    private static boolean spansMultipleSections(ArrayList<IClimbable> list) {
        int minSection = Integer.MAX_VALUE;
        int maxSection = Integer.MIN_VALUE;
        for (IClimbable c : list) {
            if (c == null) continue;
            int cMin = minSectionCoord(c);
            int cMax = maxSectionCoord(c);
            if (cMin < minSection) minSection = cMin;
            if (cMax > maxSection) maxSection = cMax;
            if (minSection < maxSection) return true;
        }
        return false;
    }

    private static boolean removeIdentity(ArrayList<IClimbable> list, IClimbable c) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == c) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    private static void collectCandidate(Level w, AABB q, ReferenceOpenHashSet<IClimbable> seen,
                                         ArrayList<IClimbable> out, IClimbable c) {
        if (c == null || !seen.add(c)) return;
        if (c.world() != w) return;
        AABB idx = c.getClimbAABBForIndexing();
        if (idx != null) {
            if (idx.intersects(q)) {
                out.add(c);
            }
            return;
        }

        BlockPos p = c.pos();
        AABB anchor = new AABB(p);
        if (anchor.intersects(q)) {
            out.add(c);
        }
    }

    private interface Bucket {
        @NotNull Bucket add(IClimbable c);

        boolean remove(IClimbable c);

        boolean isEmpty();

        boolean isEntityOnAny(Level w, LivingEntity e, int minSectionY, int maxSectionY);

        void collectIntersecting(Level w, AABB q, int minSectionY, int maxSectionY,
                                 ReferenceOpenHashSet<IClimbable> seen, ArrayList<IClimbable> out);
    }

    private static final class FlatBucket implements Bucket {
        final int cx, cz;
        final ArrayList<IClimbable> list = new ArrayList<>(1);

        private FlatBucket(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }

        @Override
        public @NotNull Bucket add(IClimbable c) {
            for (IClimbable existing : list) {
                if (existing == c) return this;
            }
            list.add(c);
            if (list.size() >= SECTION_PROMOTION_THRESHOLD && spansMultipleSections(list)) {
                return new SectionBucket(cx, cz, list);
            }
            return this;
        }

        @Override
        public boolean remove(IClimbable c) {
            return removeIdentity(list, c);
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public boolean isEntityOnAny(Level w, LivingEntity e, int minSectionY, int maxSectionY) {

            for (int i = 0; i < list.size(); i++) {
                IClimbable c = list.get(i);
                if (c == null) continue;
                if (c.world() != w) continue;
                if (c.isEntityInClimbAABB(e)) return true;
            }
            return false;
        }

        @Override
        public void collectIntersecting(Level w, AABB q, int minSectionY, int maxSectionY,
                                        ReferenceOpenHashSet<IClimbable> seen, ArrayList<IClimbable> out) {
            for (int i = 0; i < list.size(); i++) {

                collectCandidate(w, q, seen, out, list.get(i));
            }
        }
    }

    private static final class SectionBucket implements Bucket {
        final int cx, cz;
        final Long2ObjectOpenHashMap<ArrayList<IClimbable>> bySection = new Long2ObjectOpenHashMap<>();

        private SectionBucket(int cx, int cz, ArrayList<IClimbable> seed) {
            this.cx = cx;
            this.cz = cz;
            for (IClimbable c : seed) {
                add(c);
            }
        }

        @Override
        public @NotNull Bucket add(IClimbable c) {
            int minSectionY = minSectionCoord(c);
            int maxSectionY = maxSectionCoord(c);
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                long sectionKey = Library.sectionToLong(cx, cz, sectionY);
                ArrayList<IClimbable> list = bySection.get(sectionKey);
                if (list == null) {
                    list = new ArrayList<>(1);
                    bySection.put(sectionKey, list);
                }
                boolean duplicate = false;
                for (IClimbable existing : list) {
                    if (existing == c) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    list.add(c);
                }
            }
            return this;
        }

        @Override
        public boolean remove(IClimbable c) {
            boolean removed = false;
            LongOpenHashSet emptied = null;
            for (var iterator = bySection.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
                var entry = iterator.next();
                ArrayList<IClimbable> list = entry.getValue();
                if (!removeIdentity(list, c)) continue;
                removed = true;
                if (list.isEmpty()) {
                    if (emptied == null) {
                        emptied = new LongOpenHashSet();
                    }
                    emptied.add(entry.getLongKey());
                }
            }
            if (emptied != null) {
                for (long sectionKey : emptied) {
                    bySection.remove(sectionKey);
                }
            }
            return removed;
        }

        @Override
        public boolean isEmpty() {
            return bySection.isEmpty();
        }

        @Override
        public boolean isEntityOnAny(Level w, LivingEntity e, int minSectionY, int maxSectionY) {
            ReferenceOpenHashSet<IClimbable> seen = minSectionY == maxSectionY ? null : new ReferenceOpenHashSet<>();
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                ArrayList<IClimbable> list = bySection.get(Library.sectionToLong(cx, cz, sectionY));
                if (list == null || list.isEmpty()) continue;
                for (int i = 0; i < list.size(); i++) {
                    IClimbable c = list.get(i);
                    if (c == null) continue;
                    if (seen != null && !seen.add(c)) continue;
                    if (c.world() != w) continue;
                    if (c.isEntityInClimbAABB(e)) return true;
                }
            }
            return false;
        }

        @Override
        public void collectIntersecting(Level w, AABB q, int minSectionY, int maxSectionY,
                                        ReferenceOpenHashSet<IClimbable> seen, ArrayList<IClimbable> out) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                ArrayList<IClimbable> list = bySection.get(Library.sectionToLong(cx, cz, sectionY));
                if (list == null || list.isEmpty()) continue;
                for (int i = 0; i < list.size(); i++) {
                    collectCandidate(w, q, seen, out, list.get(i));
                }
            }
        }
    }

    private static final class Entry {
        final ResourceKey<Level> dim;
        final LongOpenHashSet keys = new LongOpenHashSet();

        Entry(ResourceKey<Level> dim) {
            this.dim = dim;
        }
    }
}
