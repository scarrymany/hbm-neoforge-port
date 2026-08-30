package com.hbm.explosion.vanillant.standard;

import com.hbm.packet.toclient.ExplosionRemovalSyncPacket;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PORT_SPEC performance mandate (see {@code docs/phase3/explosion_engine.md} /
 * {@code bomb_blocks_and_detonators.md}'s "Key design/API decisions"): {@code BlockAllocatorStandard}
 * can hand {@code BlockProcessorStandard} thousands of {@link BlockPos} for a large blast, and a
 * literal per-position {@code Level#setBlock} loop - fine on 1.12 - visibly stalls a 1.21.1 server,
 * whose block-change pipeline (heightmap maintenance, block-entity bookkeeping, light-engine
 * scheduling, per-tick dirty-chunk-section tracking for the network layer) is considerably heavier
 * per call. This class is the batched replacement for that one call site: group every position that
 * survived {@code BlockProcessorStandard}'s drop/mutator hooks by the {@link ChunkPos} it falls in,
 * then for each touched chunk:
 * <ol>
 *     <li>Write directly into the chunk's already-loaded {@link LevelChunkSection}s (bypassing
 *     {@code LevelChunk#setBlockState}/{@code Level#setBlock} entirely - no per-block neighbor
 *     notification, no per-block dirty-tracking, no per-block network send).</li>
 *     <li>Update just that chunk's heightmaps for the touched columns (cheap, O(1)-amortized per
 *     column per heightmap type - not the expensive part being avoided) so falling-block/mob-spawn/
 *     skylight logic that reads heightmaps stays correct.</li>
 *     <li>Queue one light-engine recheck per position via {@code LevelLightEngine#checkBlock} - this
 *     only enqueues the position, it does not force a synchronous recompute; the light engine's own
 *     already-running per-tick processing drains the queue afterward exactly as it would for any
 *     other block change, which is the "one deferred lighting-engine recalculation pass" this port's
 *     performance mandate asks for (deferred to the engine's normal batched draining, not forced
 *     inline once per block).</li>
 *     <li>Mark the chunk unsaved once.</li>
 *     <li>Send exactly one {@link ExplosionRemovalSyncPacket} for the whole chunk to nearby players,
 *     replacing the per-block client sync that direct section writes bypass (see that packet's own
 *     javadoc).</li>
 * </ol>
 * Only this final write call site changes from a naive port - {@code BlockProcessorStandard}'s
 * allocation/drop-chance/fortune/{@code IBlockMutator} logic surrounding it is unchanged.
 * <p>
 * <b>Verification status (see this package's port task's openIssues):</b> the read-side chunk-section
 * API used here ({@code LevelChunk#getSections()}, {@code Level#getMinSection()},
 * {@code LevelChunkSection#hasOnlyAir()}/{@code getBlockState}) is confirmed against this repo's own
 * Neo Edition reference ({@code com.hbm.lib.Library}'s raycast helper). The following are well-
 * established public Mojang-mapping knowledge but are <em>not</em> confirmed by any file in this repo,
 * and this sandbox cannot reach {@code maven.neoforged.net} to verify them directly - flagged here as
 * the pieces of this package that need a real-compiler spike before being trusted at nuke-tier scale,
 * per this port's explicit instruction to implement a best-effort batched design rather than fall back
 * to a naive per-block loop:
 * <ul>
 *     <li>{@code LevelChunkSection#setBlockState(int, int, int, BlockState)} - the exact write-side
 *     overload and its internal non-air block-count bookkeeping.</li>
 *     <li>{@code ChunkAccess#getHeightmaps()}/{@code Heightmap#update(int, int, int, BlockState)}.</li>
 *     <li>{@code Level#getLightEngine()}/{@code LevelLightEngine#checkBlock(BlockPos)} and whether the
 *     engine's own per-tick processing really does drain a {@code checkBlock}-queued position without
 *     an explicit "run now" call from here.</li>
 *     <li>{@code ChunkAccess#setUnsaved(boolean)} (the modern name for CE-era {@code markDirty()}).</li>
 *     <li>{@code Level#removeBlockEntity(BlockPos)} (added during review to stop a stale block-entity
 *     leak whenever a direct section write silently vaporizes a chest/machine/etc. position without
 *     going through {@code LevelChunk#setBlockState}'s own block-entity bookkeeping - see the removal
 *     loop below).</li>
 *     <li>{@code Level#getChunk(int, int)}'s exact declared return type (defensively cast to
 *     {@link LevelChunk} above rather than assumed).</li>
 *     <li>{@code ChunkPos#getMiddleBlockX()}/{@code getMiddleBlockZ()} (low-risk, standard, but still
 *     unexercised anywhere else in this repo).</li>
 * </ul>
 */
final class ChunkBatchedBlockRemoval {

    private ChunkBatchedBlockRemoval() {
    }

    /** Radius (in blocks) used to pick which nearby players receive each chunk's removal-sync packet. */
    private static final double SYNC_RANGE = 256.0D;

    static void removeAndSync(Level level, List<BlockPos> toRemove) {
        if (toRemove.isEmpty()) return;

        Map<ChunkPos, List<BlockPos>> byChunk = new HashMap<>();
        for (BlockPos pos : toRemove) {
            byChunk.computeIfAbsent(new ChunkPos(pos), cp -> new ArrayList<>()).add(pos.immutable());
        }

        int minSection = level.getMinSection();

        for (Map.Entry<ChunkPos, List<BlockPos>> entry : byChunk.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            List<BlockPos> positions = entry.getValue();

            // Explicit cast rather than relying on Level#getChunk(int,int)'s exact declared return type
            // (LevelChunk vs. the more general ChunkAccess) - safe either way since every position here
            // was already confirmed loaded and non-air by BlockProcessorStandard just before this call.
            LevelChunk chunk = (LevelChunk) level.getChunk(chunkPos.x, chunkPos.z);
            LevelChunkSection[] sections = chunk.getSections();
            BlockState air = Blocks.AIR.defaultBlockState();

            for (BlockPos pos : positions) {
                int sectionIndex = (pos.getY() >> 4) - minSection;
                if (sectionIndex < 0 || sectionIndex >= sections.length) continue;

                LevelChunkSection section = sections[sectionIndex];
                if (section == null || section.hasOnlyAir()) continue;

                // The direct section write below bypasses Level#setBlock entirely, which is exactly
                // the point (see class javadoc) - but that also means it bypasses LevelChunk#setBlockState's
                // own block-entity bookkeeping. Any block entity still registered at this position must be
                // torn down by hand first, or the chunk's block-entity map keeps a stale entry pointing at
                // a now-air position (a ClassCastException/NPE landmine the next time that block entity
                // ticks, and silent world-save corruption via setUnsaved(true) below).
                if (chunk.getBlockEntity(pos) != null) {
                    level.removeBlockEntity(pos);
                }

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
                        new ExplosionRemovalSyncPacket(positions));
            }
        }
    }
}
