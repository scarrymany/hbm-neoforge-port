package com.hbm.explosion;

import com.hbm.packet.toclient.NukeExplosionRemovalSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;

/**
 * PORT_SPEC's batched-block-removal mandate (see {@code docs/phase3/explosion_engine.md} §"Key
 * design/API decisions" / §"Open questions"), applied to {@link ExplosionNukeRayBatched} - the one
 * mk5 algorithm this pass ports (see that class's own javadoc for why the fully-threaded
 * {@code ExplosionNukeRayParallelized} default algorithm is explicitly deferred).
 * <p>
 * This is a standalone twin of the sibling {@code explosion_vanillant_core} package's
 * {@code ChunkBatchedBlockRemoval} (same technique, independently written - see
 * {@link com.hbm.packet.toclient.NukeExplosionRemovalSyncPacket}'s javadoc for why this package
 * doesn't just call that package-private class directly). Given one chunk's worth of positions
 * already known to be non-air (every position here survived {@code ExplosionNukeRayBatched}'s own
 * ray-resistance walk), this:
 * <ol>
 *     <li>Writes directly into the chunk's loaded {@link LevelChunkSection}s, bypassing
 *     {@code Level#setBlock}'s per-block neighbor-notification/dirty-tracking/network-send
 *     overhead - the actual fix for the "thousands of {@code Level#setBlock} calls per big nuke"
 *     performance problem PORT_SPEC calls out.</li>
 *     <li>Updates just that chunk's heightmaps per touched column (cheap; keeps
 *     skylight/mob-spawn/falling-block logic correct).</li>
 *     <li>Queues one light-engine recheck per position via {@code LevelLightEngine#checkBlock} -
 *     enqueues only; the engine's own already-running per-tick processing drains it, which is the
 *     "one deferred lighting-engine recalculation pass" PORT_SPEC asks for (never a forced
 *     synchronous recompute per block).</li>
 *     <li>Marks the chunk unsaved once.</li>
 *     <li>Sends exactly one {@link NukeExplosionRemovalSyncPacket} for the whole chunk to nearby
 *     players, replacing the per-block client sync the direct section write bypasses.</li>
 * </ol>
 * <p>
 * <b>Verification status</b> (see this port task's own {@code openIssues} / the research report's
 * "Open questions"): the read-side chunk-section API used elsewhere in this port
 * ({@code LevelChunk#getSections()}, {@code Level#getMinSection()},
 * {@code LevelChunkSection#hasOnlyAir()}/{@code getBlockState}) is confirmed against this repo's
 * own Neo Edition reference. {@code LevelChunkSection#setBlockState(int,int,int,BlockState)},
 * {@code Heightmap#update}, {@code LevelLightEngine#checkBlock}, and {@code ChunkAccess#setUnsaved}
 * are well-established public Mojang-mapping knowledge but are <em>not</em> independently
 * confirmed by any file in this repo beyond the sibling package's identical assumption - this
 * sandbox cannot reach {@code maven.neoforged.net} to verify them directly. Flagged as the piece of
 * this class that needs a real-compiler check before being trusted at nuke-tier (thousands of
 * blocks) scale, per this port's instruction to implement a best-effort batched design rather than
 * fall back to a naive per-block loop.
 */
final class NukeChunkBlockRemoval {

    private NukeChunkBlockRemoval() {
    }

    /** Radius (in blocks) used to pick which nearby players receive each chunk's removal-sync packet. */
    private static final double SYNC_RANGE = 256.0D;

    /**
     * Removes every position in {@code positions} (all known to lie within {@code chunkPos}) via a
     * direct section write, then flushes one heightmap/light/save/network pass for the whole chunk.
     * Call this exactly once per chunk, after that chunk's whole batch of positions has finished
     * draining (see {@link ExplosionNukeRayBatched#processChunkBlocks(long, int)}) - not per-tick
     * slice - so the "one deferred resync per touched chunk" shape is preserved even though a single
     * chunk's drain can itself be spread across several time-budgeted ticks.
     */
    static void removeAndSync(Level level, ChunkPos chunkPos, List<BlockPos> positions) {
        if (positions.isEmpty()) return;

        int minSection = level.getMinSection();

        // Explicit cast rather than relying on Level#getChunk(int,int)'s exact declared return
        // type (LevelChunk vs. the more general ChunkAccess) - safe since every position here was
        // already confirmed loaded and non-air by ExplosionNukeRayBatched's own ray walk.
        LevelChunk chunk = (LevelChunk) level.getChunk(chunkPos.x, chunkPos.z);
        LevelChunkSection[] sections = chunk.getSections();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (BlockPos pos : positions) {
            int sectionIndex = (pos.getY() >> 4) - minSection;
            if (sectionIndex < 0 || sectionIndex >= sections.length) continue;

            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) continue;

            section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, air);

            for (Map.Entry<Heightmap.Types, Heightmap> hmEntry : chunk.getHeightmaps()) {
                hmEntry.getValue().update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, air);
            }

            level.getLightEngine().checkBlock(pos);
        }

        chunk.setUnsaved(true);

        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null,
                    chunkPos.getMiddleBlockX(), 128, chunkPos.getMiddleBlockZ(), SYNC_RANGE,
                    new NukeExplosionRemovalSyncPacket(positions));
        }
    }
}
