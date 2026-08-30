package com.hbm.entity.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityExplosionChunkloading} (read in full, 80
 * lines). CE's version wraps 1.12/Forge's {@code ForgeChunkManager}/{@code Ticket} chunk-force-
 * loading API so a slow, multi-tick nuke explosion keeps its own chunk loaded even if every player
 * leaves the area while it's still working through its blast radius.
 * <p>
 * <b>Not ported (documented forward reference)</b>: NeoForge 1.21.1's real chunk-force-loading
 * entry point (something in the {@code ServerChunkCache}/{@code TicketType} family) is not
 * confirmed anywhere in this repository - {@code docs/phase3/explosion_engine.md}'s "Open
 * questions" flags this exact gap and this sandbox cannot reach {@code maven.neoforged.net} to
 * verify the real signature. Porting it blind risks inventing an API that doesn't compile, which
 * this project's own ground rules forbid more strongly than leaving a documented stub. Every hook
 * CE's nuke-explosion entities call ({@link #requestChunkLoaderTicketIfNeeded()}, {@link
 * #loadChunk(int, int)}, {@link #clearChunkLoader()}, {@link #markChunkLoaderRestoredFromNBT()})
 * is kept with its original name/call shape so subclasses don't need to change, but every body is
 * a documented no-op for now. Practical effect of the gap: an in-progress mk3/mk5 explosion in a
 * chunk every player has left may have its host chunk unloaded by the server before it finishes
 * (CE's {@code BombConfig.enableChunkLoading} - already ported - exists to gate exactly this
 * behavior once a real implementation lands here).
 */
public abstract class EntityExplosionChunkloading extends Entity {

    protected EntityExplosionChunkloading(EntityType<? extends EntityExplosionChunkloading> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * TODO(chunk-force-loading, see class javadoc): CE marks a just-NBT-restored entity so it
     * doesn't immediately request a *second* ticket on top of the one it should still own. Kept
     * as a documented call site for whichever pass wires up the real ticket API.
     */
    protected final void markChunkLoaderRestoredFromNBT() {
        // no-op until chunk force-loading is implemented (see class javadoc)
    }

    /** TODO(chunk-force-loading, see class javadoc): would request a real chunk-loading ticket. */
    protected final void requestChunkLoaderTicketIfNeeded() {
        // no-op until chunk force-loading is implemented (see class javadoc)
    }

    /** TODO(chunk-force-loading, see class javadoc): would force-load the given chunk coordinate. */
    public void loadChunk(int chunkX, int chunkZ) {
        // no-op until chunk force-loading is implemented (see class javadoc)
    }

    /** TODO(chunk-force-loading, see class javadoc): would release this entity's chunk ticket. */
    public void clearChunkLoader() {
        // no-op until chunk force-loading is implemented (see class javadoc)
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
