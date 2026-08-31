package com.hbm.handler.radiation;

import com.hbm.config.GeneralConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.RadFogPayload;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of CE's {@code com.hbm.handler.radiation.RadiationSystemNT} (3,946 lines in CE - see
 * {@code docs/phase4/chunk_radiation_system.md} for the full survey this class implements). CE's real
 * storage model is preserved exactly, per that report's own Headline finding: radiation is not a flat
 * per-chunk/per-block scalar, it is a per-connected-open-space "pocket" inside each 16x16x16 section,
 * discovered by a live flood fill over the section's real block palette (a wall is any block
 * implementing {@link IRadResistantBlock} whose {@code isRadResistant(...)} returns {@code true}).
 * Every section collapses to one of three representations depending on what the flood fill finds:
 * <ul>
 *     <li><b>UNI</b> - no resistant blocks at all; one {@code double} for the whole 4096-block volume.</li>
 *     <li><b>SINGLE</b> - resistant blocks exist but only one connected open pocket; one {@code double}
 *     plus the pocket's true volume ({@link SingleSectionRef}).</li>
 *     <li><b>MULTI</b> - two or more disconnected pockets; a per-block pocket-index lookup plus a
 *     density/volume pair per pocket ({@link MultiSectionRef}).</li>
 * </ul>
 * Radiation "mass" (density x volume) is conserved whenever a section is rebuilt after its block
 * palette changes: {@link #computePocketDensities} averages each new pocket's cells' <em>old</em>
 * per-cell density (looked up through whichever kind the section used to be), which is exactly
 * CE's own "join old-pocket to new-pocket by per-block overlap count, redistribute proportionally"
 * {@code remapPocketMass} algorithm expressed as a per-cell average - breaking a wall between two
 * irradiated rooms merges and redistributes their radiation instead of discarding or duplicating it.
 * <p>
 * <b>Deliberate scope reduction from CE, documented not silently dropped</b> (see this port's
 * structured report for the full list): CE runs this whole pipeline across a
 * {@code ForkJoinPool} with parity-partitioned concurrent axis sweeps, an {@code EditTable} that
 * buffers writes against not-yet-rebuilt sections for later replay, and a fine per-pocket,
 * per-shared-face-area diffusion coupling between adjacent sections (the un-read-in-full
 * {@code MultiSectionRef} face-linking bookkeeping the research report explicitly flags as
 * "concurrency/bookkeeping plumbing around already-verified algorithms, not new behavior"). This port
 * runs the whole pipeline synchronously on the server thread once per {@link RadiationConfig#RAD_TICK_RATE}
 * ticks, builds an unbuilt (dirty) section immediately on first touch rather than buffering the write
 * for later replay, and diffuses between adjacent sections using one volume-weighted representative
 * density per section (redistributed back across that section's own pockets afterward) rather than
 * resolving contact area/distance per individual pocket pair - the closed-form 2-node exchange formula
 * itself (CE's own documented {@code Δρ = (ρ_eq − ρ) × (1 − e^(−kΔt))}) and the exponential half-life
 * decay are ported exactly.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public class RadiationSystemNT {

    private static final byte KIND_NONE = 0;
    private static final byte KIND_UNI = 1;
    private static final byte KIND_SINGLE = 2;
    private static final byte KIND_MULTI = 3;

    private static final int SECTION_VOLUME = 4096;
    private static final int MAX_POCKETS = 2048;
    private static final String NBT_KEY = "hbmRadDataNT";

    private static final Map<ServerLevel, WorldRadiationData> WORLD_MAP = new HashMap<>();

    private RadiationSystemNT() {
    }

    // =====================================================================================
    // Public API - ChunkRadiationManager.proxy's real backing store.
    // =====================================================================================

    public static double getRadForCoord(ServerLevel level, BlockPos pos) {
        if (isOutsideWorld(level, pos) || isResistantAt(level, pos)) return 0D;

        WorldRadiationData data = getOrCreateWorldData(level);
        LevelChunk chunk = getLoadedChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return 0D;

        ChunkRadData cd = getOrCreateChunkData(data, level, chunk);
        int sectionIdx = (pos.getY() >> 4) - cd.minSection;
        if (sectionIdx < 0 || sectionIdx >= cd.kind.length) return 0D;

        ensureSectionBuilt(level, cd, chunk, sectionIdx);
        return readDensity(cd, sectionIdx, Library.blockPosToLocal(pos));
    }

    public static void setRadForCoord(ServerLevel level, BlockPos pos, double rad) {
        if (isOutsideWorld(level, pos) || isResistantAt(level, pos)) return;

        WorldRadiationData data = getOrCreateWorldData(level);
        LevelChunk chunk = getLoadedChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;

        ChunkRadData cd = getOrCreateChunkData(data, level, chunk);
        int sectionIdx = (pos.getY() >> 4) - cd.minSection;
        if (sectionIdx < 0 || sectionIdx >= cd.kind.length) return;

        ensureSectionBuilt(level, cd, chunk, sectionIdx);
        writeDensity(cd, sectionIdx, Library.blockPosToLocal(pos), Math.max(0D, rad));
        chunk.setUnsaved(true);
    }

    /** Uncapped variant - CE marks the matching {@code ChunkRadiationManager} overload {@code @DoNotCall}. */
    public static void incrementRad(ServerLevel level, BlockPos pos, double rad) {
        incrementRad(level, pos, rad, Double.MAX_VALUE);
    }

    public static void incrementRad(ServerLevel level, BlockPos pos, double rad, double max) {
        if (rad == 0D) return;
        if (isOutsideWorld(level, pos) || isResistantAt(level, pos)) return;

        WorldRadiationData data = getOrCreateWorldData(level);
        LevelChunk chunk = getLoadedChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;

        ChunkRadData cd = getOrCreateChunkData(data, level, chunk);
        int sectionIdx = (pos.getY() >> 4) - cd.minSection;
        if (sectionIdx < 0 || sectionIdx >= cd.kind.length) return;

        ensureSectionBuilt(level, cd, chunk, sectionIdx);
        int local = Library.blockPosToLocal(pos);
        double current = readDensity(cd, sectionIdx, local);
        writeDensity(cd, sectionIdx, local, Math.max(0D, Math.min(max, current + rad)));
        chunk.setUnsaved(true);
    }

    public static void decrementRad(ServerLevel level, BlockPos pos, double rad) {
        if (rad == 0D) return;
        if (isOutsideWorld(level, pos) || isResistantAt(level, pos)) return;

        WorldRadiationData data = getOrCreateWorldData(level);
        LevelChunk chunk = getLoadedChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;

        ChunkRadData cd = getOrCreateChunkData(data, level, chunk);
        int sectionIdx = (pos.getY() >> 4) - cd.minSection;
        if (sectionIdx < 0 || sectionIdx >= cd.kind.length) return;

        ensureSectionBuilt(level, cd, chunk, sectionIdx);
        int local = Library.blockPosToLocal(pos);
        double current = readDensity(cd, sectionIdx, local);
        writeDensity(cd, sectionIdx, local, Math.max(0D, current - rad));
        chunk.setUnsaved(true);
    }

    public static void jettisonData(ServerLevel level) {
        WORLD_MAP.remove(level);
    }

    /**
     * CE: {@code RadiationSystemNT.markSectionForRebuild(World, BlockPos)}. Every
     * {@link IRadResistantBlock} implementor must call this (or {@link #markSectionsForRebuild}) from
     * its placement/removal hooks - see that interface's own javadoc.
     */
    public static void markSectionForRebuild(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        WorldRadiationData data = getOrCreateWorldData(serverLevel);
        data.dirtySections.add(Library.blockPosToSectionLong(pos));
    }

    /** CE: {@code RadiationSystemNT.markSectionsForRebuild(World, LongIterable)}, batch form. */
    public static void markSectionsForRebuild(Level level, Iterable<BlockPos> positions) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        WorldRadiationData data = getOrCreateWorldData(serverLevel);
        for (BlockPos pos : positions) {
            data.dirtySections.add(Library.blockPosToSectionLong(pos));
        }
    }

    // =====================================================================================
    // Guards
    // =====================================================================================

    private static boolean isOutsideWorld(ServerLevel level, BlockPos pos) {
        return pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight();
    }

    private static boolean isResistantAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof IRadResistantBlock resistant && resistant.isRadResistant(level, pos);
    }

    @Nullable
    private static LevelChunk getLoadedChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (!level.hasChunk(chunkX, chunkZ)) return null;
        // Explicit cast rather than relying on Level#getChunk(int,int)'s exact declared return type
        // (LevelChunk vs. the more general ChunkAccess) - matches this port's own established,
        // defensive precedent (com.hbm.explosion.NukeChunkBlockRemoval#removeAndSync), safe either
        // way since hasChunk(...) above already confirms this chunk is loaded.
        return (LevelChunk) level.getChunk(chunkX, chunkZ);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return Library.sectionToChunkLong(Library.sectionToLong(chunkX, chunkZ, 0));
    }

    private static int sectionCount(ServerLevel level) {
        return (level.getMaxBuildHeight() - level.getMinBuildHeight()) >> 4;
    }

    private static WorldRadiationData getOrCreateWorldData(ServerLevel level) {
        return WORLD_MAP.computeIfAbsent(level, WorldRadiationData::new);
    }

    private static ChunkRadData getOrCreateChunkData(WorldRadiationData data, ServerLevel level, LevelChunk chunk) {
        long key = chunkKey(chunk.getPos().x, chunk.getPos().z);
        return data.chunks.computeIfAbsent(key,
                k -> new ChunkRadData(level.getMinSection(), sectionCount(level), chunk.getPos().x, chunk.getPos().z));
    }

    // =====================================================================================
    // Section storage read/write
    // =====================================================================================

    private static double readDensity(ChunkRadData cd, int sectionIdx, int local) {
        return switch (cd.kind[sectionIdx]) {
            case KIND_UNI, KIND_SINGLE -> cd.uniformOrSingle[sectionIdx];
            case KIND_MULTI -> {
                MultiSectionRef ref = (MultiSectionRef) cd.complex[sectionIdx];
                short p = ref.pocketIndex[local];
                yield p < 0 ? 0D : ref.density[p];
            }
            default -> 0D;
        };
    }

    private static void writeDensity(ChunkRadData cd, int sectionIdx, int local, double value) {
        switch (cd.kind[sectionIdx]) {
            case KIND_UNI, KIND_SINGLE -> cd.uniformOrSingle[sectionIdx] = value;
            case KIND_MULTI -> {
                MultiSectionRef ref = (MultiSectionRef) cd.complex[sectionIdx];
                short p = ref.pocketIndex[local];
                if (p >= 0) ref.density[p] = value;
            }
            default -> {
                // ensureSectionBuilt always runs first at every public entry point above, so a
                // write against a still-NONE section should not happen in practice.
            }
        }
    }

    private static void ensureSectionBuilt(ServerLevel level, ChunkRadData cd, LevelChunk chunk, int sectionIdx) {
        if (cd.kind[sectionIdx] != KIND_NONE) return;
        rebuildSection(level, cd, chunk, sectionIdx);
    }

    // =====================================================================================
    // Flood-fill pocket rebuild
    // =====================================================================================

    private static void rebuildSection(ServerLevel level, ChunkRadData cd, LevelChunk chunk, int sectionIdx) {
        byte oldKind = cd.kind[sectionIdx];
        double oldUniform = cd.uniformOrSingle[sectionIdx];
        SectionRef oldComplex = cd.complex[sectionIdx];

        LevelChunkSection[] sections = chunk.getSections();
        LevelChunkSection section = sectionIdx >= 0 && sectionIdx < sections.length ? sections[sectionIdx] : null;

        if (section == null || section.hasOnlyAir()) {
            collapseToUniform(cd, sectionIdx, oldKind, oldUniform, oldComplex);
            applyPendingRestore(cd, sectionIdx);
            return;
        }

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int sectionY = sectionIdx + cd.minSection;
        int baseY = sectionY << 4;

        boolean[] resistant = new boolean[SECTION_VOLUME];
        boolean anyResistant = false;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int ly = 0; ly < 16; ly++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    BlockState state = section.getBlockState(lx, ly, lz);
                    if (state.isAir()) continue;
                    if (state.getBlock() instanceof IRadResistantBlock resistantBlock) {
                        mpos.set(baseX + lx, baseY + ly, baseZ + lz);
                        if (resistantBlock.isRadResistant(level, mpos)) {
                            resistant[(ly << 8) | (lz << 4) | lx] = true;
                            anyResistant = true;
                        }
                    }
                }
            }
        }

        if (!anyResistant) {
            collapseToUniform(cd, sectionIdx, oldKind, oldUniform, oldComplex);
            applyPendingRestore(cd, sectionIdx);
            return;
        }

        int[] pocketOf = new int[SECTION_VOLUME];
        java.util.Arrays.fill(pocketOf, -1);
        List<Integer> volumes = new ArrayList<>();
        java.util.ArrayDeque<Integer> bfs = new java.util.ArrayDeque<>();

        for (int start = 0; start < SECTION_VOLUME; start++) {
            if (resistant[start] || pocketOf[start] >= 0) continue;

            int id = volumes.size();
            if (id >= MAX_POCKETS) {
                pocketOf[start] = MAX_POCKETS - 1;
                continue;
            }

            int count = 0;
            bfs.add(start);
            pocketOf[start] = id;
            while (!bfs.isEmpty()) {
                int cur = bfs.poll();
                count++;
                int lx = cur & 15, lz = (cur >> 4) & 15, ly = (cur >> 8) & 15;

                if (lx > 0) visitNeighbor(cur - 1, resistant, pocketOf, id, bfs);
                if (lx < 15) visitNeighbor(cur + 1, resistant, pocketOf, id, bfs);
                if (lz > 0) visitNeighbor(cur - 16, resistant, pocketOf, id, bfs);
                if (lz < 15) visitNeighbor(cur + 16, resistant, pocketOf, id, bfs);
                if (ly > 0) visitNeighbor(cur - 256, resistant, pocketOf, id, bfs);
                if (ly < 15) visitNeighbor(cur + 256, resistant, pocketOf, id, bfs);
            }
            volumes.add(count);
        }

        int pocketCount = volumes.size();

        if (pocketCount == 0) {
            // Fully walled section - no open cells exist for anything to ever query (isResistantAt
            // short-circuits every public entry point before it would reach this section's data).
            // Any pending chunk-load restore data for this section is discarded here rather than
            // left to linger and be misapplied against a much later rebuild once the section opens
            // back up again.
            cd.pendingRestore.remove(sectionIdx);
            cd.kind[sectionIdx] = KIND_UNI;
            cd.uniformOrSingle[sectionIdx] = 0D;
            cd.complex[sectionIdx] = null;
            return;
        }

        double[] newDensities = computePocketDensities(oldKind, oldUniform, oldComplex, pocketOf, pocketCount);
        applyPendingRestoreToPockets(cd, sectionIdx, newDensities);

        if (pocketCount == 1) {
            cd.kind[sectionIdx] = KIND_SINGLE;
            cd.uniformOrSingle[sectionIdx] = newDensities[0];
            SingleSectionRef ref = new SingleSectionRef();
            ref.volume = volumes.get(0);
            cd.complex[sectionIdx] = ref;
            return;
        }

        MultiSectionRef ref = new MultiSectionRef();
        ref.pocketIndex = new short[SECTION_VOLUME];
        for (int i = 0; i < SECTION_VOLUME; i++) {
            ref.pocketIndex[i] = (short) pocketOf[i];
        }
        ref.density = newDensities;
        ref.volume = new int[pocketCount];
        for (int p = 0; p < pocketCount; p++) ref.volume[p] = volumes.get(p);

        cd.kind[sectionIdx] = KIND_MULTI;
        cd.uniformOrSingle[sectionIdx] = 0D;
        cd.complex[sectionIdx] = ref;
    }

    private static void visitNeighbor(int idx, boolean[] resistant, int[] pocketOf, int id, java.util.ArrayDeque<Integer> bfs) {
        if (resistant[idx] || pocketOf[idx] >= 0) return;
        pocketOf[idx] = id;
        bfs.add(idx);
    }

    private static void collapseToUniform(ChunkRadData cd, int sectionIdx, byte oldKind, double oldUniform, @Nullable SectionRef oldComplex) {
        double density = averageOldDensityAll(oldKind, oldUniform, oldComplex);
        cd.kind[sectionIdx] = KIND_UNI;
        cd.uniformOrSingle[sectionIdx] = density;
        cd.complex[sectionIdx] = null;
    }

    private static double averageOldDensityAll(byte oldKind, double oldUniform, @Nullable SectionRef oldComplex) {
        if (oldKind == KIND_UNI || oldKind == KIND_SINGLE) return oldUniform;
        if (oldKind == KIND_MULTI && oldComplex instanceof MultiSectionRef ref) {
            double sum = 0D;
            for (int local = 0; local < SECTION_VOLUME; local++) {
                short p = ref.pocketIndex[local];
                if (p >= 0 && p < ref.density.length) sum += ref.density[p];
            }
            return sum / SECTION_VOLUME;
        }
        return 0D;
    }

    /**
     * CE's {@code remapPocketMass}, expressed as a per-cell average: each new pocket's density is the
     * mean of the <em>old</em> per-cell density of every cell it now contains, which is exactly the
     * old-pocket-to-new-pocket overlap-weighted mass redistribution CE performs (a cell dropped from
     * consideration because a new resistant block now occupies it simply does not contribute, matching
     * CE's own behavior of not conserving mass into newly-sealed walls).
     */
    private static double[] computePocketDensities(byte oldKind, double oldUniform, @Nullable SectionRef oldComplex, int[] pocketOf, int pocketCount) {
        double[] sum = new double[pocketCount];
        int[] count = new int[pocketCount];
        for (int local = 0; local < SECTION_VOLUME; local++) {
            int p = pocketOf[local];
            if (p < 0 || p >= pocketCount) continue;
            double oldDensity = oldDensityAt(oldKind, oldUniform, oldComplex, local);
            sum[p] += oldDensity;
            count[p]++;
        }
        double[] result = new double[pocketCount];
        for (int p = 0; p < pocketCount; p++) {
            result[p] = count[p] > 0 ? sum[p] / count[p] : 0D;
        }
        return result;
    }

    private static double oldDensityAt(byte oldKind, double oldUniform, @Nullable SectionRef oldComplex, int local) {
        return switch (oldKind) {
            case KIND_UNI, KIND_SINGLE -> oldUniform;
            case KIND_MULTI -> {
                if (!(oldComplex instanceof MultiSectionRef ref)) yield 0D;
                short p = ref.pocketIndex[local];
                yield (p < 0 || p >= ref.density.length) ? 0D : ref.density[p];
            }
            default -> 0D;
        };
    }

    // =====================================================================================
    // Chunk-load restore (persisted per-pocket densities, applied once topology is known)
    // =====================================================================================

    private static void applyPendingRestore(ChunkRadData cd, int sectionIdx) {
        Int2DoubleOpenHashMap saved = cd.pendingRestore.remove(sectionIdx);
        if (saved == null || saved.isEmpty()) return;
        double v = saved.get(0);
        if (v != 0D) cd.uniformOrSingle[sectionIdx] = v;
    }

    private static void applyPendingRestoreToPockets(ChunkRadData cd, int sectionIdx, double[] densities) {
        Int2DoubleOpenHashMap saved = cd.pendingRestore.remove(sectionIdx);
        if (saved == null || saved.isEmpty()) return;
        for (Int2DoubleMap.Entry entry : saved.int2DoubleEntrySet()) {
            int pocket = entry.getIntKey();
            if (pocket >= 0 && pocket < densities.length) {
                densities[pocket] = entry.getDoubleValue();
            }
        }
    }

    // =====================================================================================
    // Tick pipeline
    // =====================================================================================

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!GeneralConfig.ENABLE_RADIATION.get() || !GeneralConfig.ENABLE_ADVANCED_RADIATION.get()) return;

        int rate = Math.max(1, RadiationConfig.RAD_TICK_RATE.get());
        tickCounter++;
        if (tickCounter < rate) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            WorldRadiationData data = getOrCreateWorldData(level);
            processWorldSimulation(level, data);
        }
    }

    private static void processWorldSimulation(ServerLevel level, WorldRadiationData data) {
        rebuildDirtySections(level, data);
        runExactExchangeSweeps(level, data);
        postSweepDecayAndEffects(level, data);
        handleWorldDestruction(level, data);
    }

    private static void rebuildDirtySections(ServerLevel level, WorldRadiationData data) {
        if (data.dirtySections.isEmpty()) return;
        long[] keys = data.dirtySections.toLongArray();
        data.dirtySections.clear();

        for (long sectionKey : keys) {
            int cx = Library.getSectionX(sectionKey);
            int cz = Library.getSectionZ(sectionKey);
            int sy = Library.getSectionY(sectionKey);

            LevelChunk chunk = getLoadedChunk(level, cx, cz);
            if (chunk == null) continue; // rebuilt lazily on next query once/if reloaded

            ChunkRadData cd = getOrCreateChunkData(data, level, chunk);
            int sectionIdx = sy - cd.minSection;
            if (sectionIdx < 0 || sectionIdx >= cd.kind.length) continue;

            rebuildSection(level, cd, chunk, sectionIdx);
        }
    }

    /**
     * CE's three rotating axis sweeps (X between east/west chunk neighbors, Z between north/south,
     * Y between vertically-adjacent sections in the same column). This port exchanges one
     * volume-weighted representative density per section rather than CE's fine per-pocket,
     * per-shared-face coupling - see class javadoc.
     */
    private static void runExactExchangeSweeps(ServerLevel level, WorldRadiationData data) {
        long epoch = level.getGameTime() / Math.max(1, RadiationConfig.RAD_TICK_RATE.get());
        int[] order = AXIS_ORDERS[(int) (epoch % AXIS_ORDERS.length)];
        for (int axis : order) {
            switch (axis) {
                case 0 -> sweepHorizontal(data, 1, 0);
                case 1 -> sweepHorizontal(data, 0, 1);
                case 2 -> sweepVertical(data);
            }
        }
    }

    private static final int[][] AXIS_ORDERS = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
    };

    private static void sweepHorizontal(WorldRadiationData data, int dx, int dz) {
        for (ChunkRadData cd : new ArrayList<>(data.chunks.values())) {
            long neighborKey = chunkKey(cd.chunkX + dx, cd.chunkZ + dz);
            ChunkRadData neighbor = data.chunks.get(neighborKey);
            if (neighbor == null) continue;

            int sections = Math.min(cd.kind.length, neighbor.kind.length);
            for (int s = 0; s < sections; s++) {
                if (cd.kind[s] == KIND_NONE || neighbor.kind[s] == KIND_NONE) continue;
                exchangeSections(cd, s, neighbor, s);
            }
        }
    }

    private static void sweepVertical(WorldRadiationData data) {
        for (ChunkRadData cd : data.chunks.values()) {
            for (int s = 0; s + 1 < cd.kind.length; s++) {
                if (cd.kind[s] == KIND_NONE || cd.kind[s + 1] == KIND_NONE) continue;
                exchangeSections(cd, s, cd, s + 1);
            }
        }
    }

    /** {volume-weighted density, total volume} for one section, across every kind. */
    private static double[] representative(ChunkRadData cd, int s) {
        return switch (cd.kind[s]) {
            case KIND_UNI -> new double[]{cd.uniformOrSingle[s], SECTION_VOLUME};
            case KIND_SINGLE -> new double[]{cd.uniformOrSingle[s], ((SingleSectionRef) cd.complex[s]).volume};
            case KIND_MULTI -> {
                MultiSectionRef ref = (MultiSectionRef) cd.complex[s];
                double mass = 0D;
                int volume = 0;
                for (int p = 0; p < ref.density.length; p++) {
                    mass += ref.density[p] * ref.volume[p];
                    volume += ref.volume[p];
                }
                yield new double[]{volume > 0 ? mass / volume : 0D, volume};
            }
            default -> new double[]{0D, 0D};
        };
    }

    /**
     * CE's own documented closed-form 2-node diffusion step: volume-weighted equilibrium
     * {@code r* = (ρ_A/V_B + ρ_B/V_A)/(1/V_A+1/V_B)}, each side relaxing toward it by
     * {@code e^(-diffusionDt/128)} (CE's cached {@code UU_E} constant for the full-face
     * uniform-uniform case, applied here to every section pair - see class javadoc).
     */
    private static void exchangeSections(ChunkRadData a, int sa, ChunkRadData b, int sb) {
        double[] ra = representative(a, sa);
        double[] rb = representative(b, sb);
        double da = ra[0], va = ra[1], db = rb[0], vb = rb[1];
        if (va <= 0D || vb <= 0D) return;

        double diffusionDt = RadiationConfig.RAD_DIFFUSIVITY.get() * (RadiationConfig.RAD_TICK_RATE.get() / 20.0D);
        double relax = Math.exp(-diffusionDt / 128.0D);
        double eq = (da / vb + db / va) / (1.0D / va + 1.0D / vb);
        double newDa = eq + (da - eq) * relax;
        double newDb = eq + (db - eq) * relax;

        applyRepresentativeDelta(a, sa, da, newDa);
        applyRepresentativeDelta(b, sb, db, newDb);
    }

    private static void applyRepresentativeDelta(ChunkRadData cd, int s, double oldRep, double newRep) {
        switch (cd.kind[s]) {
            case KIND_UNI, KIND_SINGLE -> cd.uniformOrSingle[s] = Math.max(0D, newRep);
            case KIND_MULTI -> {
                MultiSectionRef ref = (MultiSectionRef) cd.complex[s];
                if (oldRep <= 1e-12D) {
                    java.util.Arrays.fill(ref.density, Math.max(0D, newRep));
                } else {
                    double scale = newRep / oldRep;
                    for (int p = 0; p < ref.density.length; p++) {
                        ref.density[p] = Math.max(0D, ref.density[p] * scale);
                    }
                }
            }
            default -> {
            }
        }
    }

    /**
     * CE: {@code postSweepDecayAndEffects} - exponential half-life decay
     * ({@code next = density * e^(ln(0.5) * (tickRate/20) / halfLifeSeconds)}), then per-pocket
     * fog/world-destruction checks.
     */
    private static void postSweepDecayAndEffects(ServerLevel level, WorldRadiationData data) {
        double retention = retentionFactor();

        for (ChunkRadData cd : data.chunks.values()) {
            for (int s = 0; s < cd.kind.length; s++) {
                byte kind = cd.kind[s];
                if (kind == KIND_NONE) continue;

                if (kind == KIND_UNI || kind == KIND_SINGLE) {
                    double v = cd.uniformOrSingle[s];
                    if (v <= 0D) continue;
                    v *= retention;
                    cd.uniformOrSingle[s] = v;
                    checkFogAndDestruction(level, data, cd, s, 0, v);
                } else {
                    MultiSectionRef ref = (MultiSectionRef) cd.complex[s];
                    for (int p = 0; p < ref.density.length; p++) {
                        double v = ref.density[p];
                        if (v <= 0D) continue;
                        v *= retention;
                        ref.density[p] = v;
                        checkFogAndDestruction(level, data, cd, s, p, v);
                    }
                }
            }
        }
    }

    private static double retentionFactor() {
        double halfLife = RadiationConfig.RAD_HALF_LIFE_SECONDS.get();
        if (halfLife <= 0D) return 0D;
        double tickRate = RadiationConfig.RAD_TICK_RATE.get();
        return Math.exp(Math.log(0.5D) * (tickRate / 20.0D) / halfLife);
    }

    private static void checkFogAndDestruction(ServerLevel level, WorldRadiationData data, ChunkRadData cd, int sectionIdx, int pocketIndex, double density) {
        RandomSource random = level.getRandom();

        if (density >= RadiationConfig.FOG_THRESHOLD.get() && random.nextInt(Math.max(1, RadiationConfig.FOG_CHANCE.get())) == 0) {
            spawnFog(level, cd, sectionIdx, pocketIndex);
        }

        if (RadiationConfig.WORLD_RAD_EFFECTS.get() && density >= 5.0D && random.nextInt(100) == 0) {
            data.pocketToDestroy = packDestroyKey(cd.chunkX, cd.chunkZ, sectionIdx, pocketIndex);
        }
    }

    private static void spawnFog(ServerLevel level, ChunkRadData cd, int sectionIdx, int pocketIndex) {
        int baseX = cd.chunkX << 4;
        int baseZ = cd.chunkZ << 4;
        int baseY = (sectionIdx + cd.minSection) << 4;

        int local = 8 | (8 << 4) | (8 << 8); // section center fallback (uniform/single kind)
        if (cd.kind[sectionIdx] == KIND_MULTI) {
            MultiSectionRef ref = (MultiSectionRef) cd.complex[sectionIdx];
            for (int i = 0; i < SECTION_VOLUME; i++) {
                if (ref.pocketIndex[i] == pocketIndex) {
                    local = i;
                    break;
                }
            }
        }

        double x = baseX + Library.getLocalX(local) + 0.5D;
        double y = baseY + Library.getLocalY(local) + 0.5D;
        double z = baseZ + Library.getLocalZ(local) + 0.5D;

        PacketDistributor.sendToPlayersNear(level, null, x, y, z, 100, new RadFogPayload(x, y, z));
    }

    /**
     * 22 bits per chunk axis (+-2,097,151) - deliberately wider than a naive 21-bit field, which
     * would only cover +-1,048,576 and silently wrap/corrupt for chunks near the real vanilla world
     * border (+-30,000,000 blocks, i.e. chunk coordinates up to ~+-1,875,000).
     */
    private static long packDestroyKey(int chunkX, int chunkZ, int sectionIdx, int pocketIndex) {
        return (((long) chunkX) & 0x3FFFFFL)
                | ((((long) chunkZ) & 0x3FFFFFL) << 22)
                | (((long) sectionIdx & 0xFFL) << 44)
                | (((long) pocketIndex & 0x7FFL) << 52);
    }

    private static int signExtend22(long v) {
        v &= 0x3FFFFFL;
        if ((v & 0x200000L) != 0) v -= (1L << 22);
        return (int) v;
    }

    private static void handleWorldDestruction(ServerLevel level, WorldRadiationData data) {
        if (!RadiationConfig.WORLD_RAD_EFFECTS.get()) return;

        long key = data.pocketToDestroy;
        data.pocketToDestroy = Long.MIN_VALUE;
        if (key == Long.MIN_VALUE) return;

        int chunkX = signExtend22(key);
        int chunkZ = signExtend22(key >>> 22);
        int sectionIdx = (int) ((key >>> 44) & 0xFFL);
        int pocketIndex = (int) ((key >>> 52) & 0x7FFL);

        destroySectionPocket(level, data, chunkX, chunkZ, sectionIdx, pocketIndex);
    }

    private static void destroySectionPocket(ServerLevel level, WorldRadiationData data, int chunkX, int chunkZ, int sectionIdx, int pocketIndex) {
        ChunkRadData cd = data.chunks.get(chunkKey(chunkX, chunkZ));
        if (cd == null || sectionIdx < 0 || sectionIdx >= cd.kind.length) return;
        byte kind = cd.kind[sectionIdx];
        if (kind == KIND_NONE) return;

        LevelChunk chunk = getLoadedChunk(level, chunkX, chunkZ);
        if (chunk == null) return;
        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIdx >= sections.length) return;
        LevelChunkSection section = sections[sectionIdx];
        if (section == null || section.hasOnlyAir()) return;

        MultiSectionRef multi = kind == KIND_MULTI ? (MultiSectionRef) cd.complex[sectionIdx] : null;

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int baseY = (sectionIdx + cd.minSection) << 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        RandomSource random = level.getRandom();

        for (int local = 0; local < SECTION_VOLUME; local++) {
            if (random.nextInt(3) != 0) continue;

            if (multi != null) {
                if (multi.pocketIndex[local] != pocketIndex) continue;
            } else if (pocketIndex != 0) {
                continue;
            }

            int lx = Library.getLocalX(local), lz = Library.getLocalZ(local), ly = Library.getLocalY(local);
            BlockState state = section.getBlockState(lx, ly, lz);
            if (state.isAir()) continue;

            int worldX = baseX + lx;
            int worldZ = baseZ + lz;
            int worldY = baseY + ly;
            int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
            if (worldY < topY - 1 || worldY > topY) continue;

            pos.set(worldX, worldY, worldZ);
            RadiationWorldHandler.decayBlock(level, pos, state);
        }
    }

    // =====================================================================================
    // Persistence: ChunkDataEvent.Load/Save. See docs/phase4/chunk_radiation_system.md's Open
    // questions - whether ChunkAccess implements IAttachmentHolder in this NeoForge version is
    // unverified in this sandbox, so this uses the confirmed-safe ChunkDataEvent NBT hook instead
    // (also confirmed real by neo-edition's own independent chunk-radiation port using the same hook).
    // =====================================================================================

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            getOrCreateWorldData(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WORLD_MAP.remove(level);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        WorldRadiationData data = WORLD_MAP.get(level);
        if (data == null) return;
        ChunkPos pos = event.getChunk().getPos();
        data.chunks.remove(chunkKey(pos.x, pos.z));
    }

    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ChunkAccess chunk = event.getChunk();
        WorldRadiationData data = getOrCreateWorldData(level);
        ChunkPos pos = chunk.getPos();
        ChunkRadData cd = new ChunkRadData(level.getMinSection(), sectionCount(level), pos.x, pos.z);

        CompoundTag tag = event.getData();
        if (tag.contains(NBT_KEY)) {
            decodePayload(tag.getByteArray(NBT_KEY), cd);
        }

        data.chunks.put(chunkKey(pos.x, pos.z), cd);
    }

    @SubscribeEvent
    public static void onChunkDataSave(ChunkDataEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        WorldRadiationData data = WORLD_MAP.get(level);
        if (data == null) return;

        ChunkAccess chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        ChunkRadData cd = data.chunks.get(chunkKey(pos.x, pos.z));
        if (cd == null) return;

        byte[] payload = encodePayload(cd);
        if (payload.length > 2) {
            event.getData().putByteArray(NBT_KEY, payload);
        }
    }

    /** 2-byte entry count, then repeated (2-byte sectionIdx&lt;&lt;11|pocketIndex, 8-byte density). */
    private static byte[] encodePayload(ChunkRadData cd) {
        List<int[]> keys = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (int s = 0; s < cd.kind.length; s++) {
            byte kind = cd.kind[s];
            if (kind == KIND_UNI || kind == KIND_SINGLE) {
                double v = cd.uniformOrSingle[s];
                if (v != 0D) {
                    keys.add(new int[]{s, 0});
                    values.add(v);
                }
            } else if (kind == KIND_MULTI) {
                MultiSectionRef ref = (MultiSectionRef) cd.complex[s];
                for (int p = 0; p < ref.density.length; p++) {
                    if (ref.density[p] != 0D) {
                        keys.add(new int[]{s, p});
                        values.add(ref.density[p]);
                    }
                }
            }
        }

        int count = keys.size();
        ByteBuffer buf = ByteBuffer.allocate(2 + count * 10);
        buf.putShort((short) count);
        for (int i = 0; i < count; i++) {
            int[] k = keys.get(i);
            int packed = (k[0] << 11) | (k[1] & 0x7FF);
            buf.putShort((short) packed);
            buf.putDouble(values.get(i));
        }
        return buf.array();
    }

    private static void decodePayload(byte[] data, ChunkRadData cd) {
        if (data == null || data.length < 2) return;
        ByteBuffer buf = ByteBuffer.wrap(data);
        int count = Short.toUnsignedInt(buf.getShort());

        for (int i = 0; i < count && buf.remaining() >= 10; i++) {
            int packed = Short.toUnsignedInt(buf.getShort());
            double density = buf.getDouble();
            int sectionIdx = packed >>> 11;
            int pocketIdx = packed & 0x7FF;
            if (sectionIdx < 0 || sectionIdx >= cd.kind.length) continue;

            cd.pendingRestore.computeIfAbsent(sectionIdx, k -> new Int2DoubleOpenHashMap()).put(pocketIdx, density);
        }
    }

    // =====================================================================================
    // Data structures
    // =====================================================================================

    private static final class WorldRadiationData {
        final Long2ObjectOpenHashMap<ChunkRadData> chunks = new Long2ObjectOpenHashMap<>();
        final LongOpenHashSet dirtySections = new LongOpenHashSet();
        long pocketToDestroy = Long.MIN_VALUE;

        WorldRadiationData(ServerLevel level) {
            // level itself is not retained - every call site already has its own ServerLevel in hand
            // (event objects, tick loop, or the query API's own parameter), so this instance only
            // needs to exist as a per-level bucket, not to remember which level it belongs to.
        }
    }

    private static final class ChunkRadData {
        final int minSection;
        final int chunkX;
        final int chunkZ;
        final byte[] kind;
        final double[] uniformOrSingle;
        final SectionRef[] complex;
        final Map<Integer, Int2DoubleOpenHashMap> pendingRestore = new HashMap<>();

        ChunkRadData(int minSection, int sectionCount, int chunkX, int chunkZ) {
            this.minSection = minSection;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.kind = new byte[sectionCount];
            this.uniformOrSingle = new double[sectionCount];
            this.complex = new SectionRef[sectionCount];
        }
    }

    private interface SectionRef {
    }

    private static final class SingleSectionRef implements SectionRef {
        int volume;
    }

    private static final class MultiSectionRef implements SectionRef {
        short[] pocketIndex;
        double[] density;
        int[] volume;
    }
}
