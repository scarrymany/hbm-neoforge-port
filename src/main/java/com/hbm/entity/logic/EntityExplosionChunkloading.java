package com.hbm.entity.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityExplosionChunkloading} (read in full, 80
 * lines). CE's version wraps 1.12/Forge's {@code ForgeChunkManager}/{@code Ticket} chunk-force-
 * loading API so a slow, multi-tick nuke explosion keeps its own chunk loaded even if every player
 * leaves the area while it's still working through its blast radius. CE's {@code loadChunk(x, z)}
 * only ever forces the chunk once (guarded by {@code this.loadedChunk == null} - CE's own nuke
 * entities never move, so there is nothing to re-force), and {@code requestChunkLoaderTicketIfNeeded}
 * only requests its single {@code Ticket} once as well.
 * <p>
 * <b>Wired to a real, already-confirmed 1.21.1 API</b> ({@link IChunkLoader}, whose own javadoc
 * verifies {@link ServerLevel#setChunkForced(int, int, boolean)} against this repo's own
 * {@code com.hbm.entity.projectile.EntityBulletBaseMK4CL} - a sibling class in this same codebase
 * that force-loads chunks via the identical call today). 1.21.1's {@code setChunkForced} needs no
 * persistent {@code Ticket} object to restore across NBT (re)loads, so {@code
 * markChunkLoaderRestoredFromNBT}'s only remaining job is staying a documented no-op call site -
 * {@link #loadedChunkPos} is deliberately not persisted to NBT; a reloaded entity simply re-forces
 * its current chunk on its very next tick, which converges to the same end state CE's restore flag
 * was protecting against (a leaked double ticket), without needing to protect against anything here.
 * <p>
 * CE defines a {@code BombConfig.enableChunkLoading} toggle but - confirmed by reading CE's
 * {@code BombConfig} in full - never actually consults it anywhere outside its own definition; this
 * port matches that real (if seemingly unintended) CE behavior by always chunk-loading rather than
 * gating on a config value CE itself never wired up.
 */
public abstract class EntityExplosionChunkloading extends Entity implements IChunkLoader {

    @Nullable
    private ChunkPos loadedChunkPos;

    protected EntityExplosionChunkloading(EntityType<? extends EntityExplosionChunkloading> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setLoadedChunkPos(ChunkPos pos) {
        this.loadedChunkPos = pos;
    }

    @Override
    @Nullable
    public ChunkPos getLoadedChunkPos() {
        return this.loadedChunkPos;
    }

    /**
     * CE marks a just-NBT-restored entity so it doesn't immediately request a *second* ticket on
     * top of the one it should still own - see class javadoc for why 1.21.1's {@code
     * setChunkForced} needs no equivalent guard.
     */
    protected final void markChunkLoaderRestoredFromNBT() {
        // no-op: see class javadoc.
    }

    /**
     * CE's ticket-request hook; 1.21.1's {@code setChunkForced} needs no separate "request a
     * ticket" step, so this stays a no-op and {@link #loadChunk(int, int)} does the actual work.
     */
    protected final void requestChunkLoaderTicketIfNeeded() {
        // no-op: see class javadoc.
    }

    /**
     * Force-loads the given chunk coordinate exactly once (matching CE's {@code loadedChunk == null}
     * guard - this port's nuke-tier entities never move, so there is nothing to re-force or unforce
     * on subsequent calls).
     */
    public void loadChunk(int chunkX, int chunkZ) {
        if (this.loadedChunkPos != null) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        this.loadedChunkPos = new ChunkPos(chunkX, chunkZ);
        serverLevel.setChunkForced(chunkX, chunkZ, true);
    }

    /** Releases this entity's forced-chunk hold, if it is holding one. */
    public void clearChunkLoader() {
        if (this.loadedChunkPos == null) return;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.setChunkForced(this.loadedChunkPos.x, this.loadedChunkPos.z, false);
        }
        this.loadedChunkPos = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            requestChunkLoaderTicketIfNeeded();
        }
    }

    /** None of CE's three subclasses (mk3/mk5/balefire) use synced entity data. */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
