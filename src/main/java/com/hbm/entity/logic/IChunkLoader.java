package com.hbm.entity.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.UUID;

/**
 * Replacement for CE's {@code com.hbm.entity.logic.IChunkLoader} (a thin contract over 1.12 Forge's
 * ticket-based {@code ForgeChunkManager} API - {@code init(Ticket)}/
 * {@code loadNeighboringChunks(int,int)}). 1.12's raw ticket-registration model has no 1:1 1.21.1
 * NeoForge analogue, so this interface is re-expressed directly against NeoForge 1.21.1's own
 * {@link ServerLevel#setChunkForced(int, int, boolean)} convenience (a NeoForge-added wrapper over
 * the vanilla {@link TicketType}/{@code DistanceManager} forced-chunk system that plays the same
 * role CE's ticket object did) rather than attempting a literal 1:1 method-shape translation.
 * <p>
 * Any entity that needs to keep its own chunk (and only its own chunk - see
 * {@link #updateChunkTicket}, which does not force a 3x3 neighborhood the way CE's
 * {@code loadNeighboringChunks} name implied) force-loaded while it flies or otherwise exists across
 * chunk boundaries (missiles, long-range bullets, slow multi-tick explosions, etc.) implements this
 * and calls the three lifecycle hooks from its own {@code onAddedToLevel}/{@code onRemovedFromLevel}/
 * {@code tick()} overrides.
 * <p>
 * {@link #ENTITY} is exposed for implementations that need a raw {@link TicketType}-keyed ticket
 * directly (e.g. if a future consumer wants {@code addRegionTicket}/{@code removeRegionTicket}
 * instead of the simpler {@code setChunkForced} wrapper the default methods below use) - the default
 * methods themselves do not need it, since {@code setChunkForced} already manages its own internal
 * ticket bookkeeping.
 */
public interface IChunkLoader {

    TicketType<UUID> ENTITY = TicketType.create("entity", Comparator.comparing(UUID::toString));

    void setLoadedChunkPos(ChunkPos pos);

    ChunkPos getLoadedChunkPos();

    default void onAddedToLevel(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            this.setLoadedChunkPos(new ChunkPos(entity.blockPosition()));
            serverLevel.setChunkForced(getLoadedChunkPos().x, getLoadedChunkPos().z, true);
        }
    }

    /** Mirror of {@link #onAddedToLevel} - unforces the chunk this entity is currently holding. */
    default void onRemovedFromLevel(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            ChunkPos pos = getLoadedChunkPos();
            serverLevel.setChunkForced(pos.x, pos.z, false);
        }
    }

    /**
     * Called each tick an implementing entity moves. Diffs the entity's current chunk against the
     * chunk it last force-loaded; if they differ, forces the new chunk, unforces the old one (in
     * that order, so there is never a tick where neither chunk is forced), and records the new
     * position via {@link #setLoadedChunkPos}. A no-op when the entity has not crossed into a new
     * chunk since the last call.
     */
    default void updateChunkTicket(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            ChunkPos oldPos = getLoadedChunkPos();
            ChunkPos newPos = new ChunkPos(entity.blockPosition());

            if (!newPos.equals(oldPos)) {
                serverLevel.setChunkForced(newPos.x, newPos.z, true);
                serverLevel.setChunkForced(oldPos.x, oldPos.z, false);
                this.setLoadedChunkPos(newPos);
            }
        }
    }
}
