package com.hbm.entity.projectile;

import com.hbm.entity.GunEntityTypes;
import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.entity.projectile.EntityBulletBaseMK4CL} (107 lines) - the "chunk-
 * loading" bullet variant, so long-range artillery-fired projectiles crossing unloaded chunks don't
 * despawn mid-flight. Read in full per this task's instruction (the gun-framework report explicitly
 * flagged it as sized/diffed but not read function-by-function) - CE's actual mechanism is a
 * {@code ForgeChunkManager.Ticket} bound to the entity, re-forced to the entity's current chunk every
 * tick (via {@code requestChunkLoaderTicketIfNeeded}/{@code loadNeighboringChunks}, called from
 * {@code onUpdate}), released in {@code setDead}/{@code clearChunkLoader}. Despite the plural method
 * name, CE's {@code loadNeighboringChunks} only ever force-loads the bullet's own current chunk, not
 * a 3x3 neighborhood - ported with that same single-chunk scope, not "fixed" into a neighborhood
 * load.
 * <p>
 * <b>No {@code IChunkLoader} interface exists in this port yet</b> - the parallel
 * {@code entity_logic_utilities} package (which owns a general chunk-loading contract) had not
 * landed {@code IChunkLoader} at the time this file was written (confirmed by checking
 * {@code com.hbm.entity.logic} in this repo - only {@code EntityExplosionChunkloading}/
 * {@code EntityNukeExplosionMK5} exist there, and the former's own javadoc stubs its chunk-loading as
 * a documented no-op citing an unconfirmed API). This class instead force-loads chunks directly via
 * {@link ServerLevel#setChunkForced(int, int, boolean)} - a confirmed real NeoForge 1.21.1 API
 * (verified against {@code upstream/neo-edition/src/main/java/com/hbm/entity/logic/IChunkLoader.java},
 * a real working parallel-port file using this exact 3-arg overload; that same file also declares an
 * unused {@code TicketType<UUID>} constant it never actually passes to {@code setChunkForced}, which
 * this port omits as dead weight rather than copy verbatim). <b>Open issue for whoever lands
 * {@code entity_logic_utilities}'s {@code IChunkLoader}</b>: reconcile this class's inline
 * force-loading with that interface once it exists, so there is exactly one chunk-loading mechanism
 * in the port rather than two independently-invented ones (see this task's own structured output for
 * the same note).
 * <p>
 * CE's NBT-restore dance ({@code awaitingTicketRestore}, needed because Forge's {@code Ticket}
 * objects couldn't just be silently re-requested on every load without leaking) has no equivalent
 * need here: {@code setChunkForced}'s simple int-pair-plus-boolean shape has no persistent ticket
 * object to restore - the chunk position is simply recomputed and re-forced on this entity's very
 * next tick after being loaded from NBT, which is a strictly faster convergence to the same end state
 * CE's restore flag was protecting.
 */
public class EntityBulletBaseMK4CL extends EntityBulletBaseMK4 {

    @Nullable
    private ChunkPos loadedChunkPos;

    public EntityBulletBaseMK4CL(EntityType<? extends EntityBulletBaseMK4CL> type, Level level) {
        super(type, level);
    }

    public EntityBulletBaseMK4CL(Level level) {
        this(GunEntityTypes.BULLET_MK4CL.get(), level);
    }

    public EntityBulletBaseMK4CL(LivingEntity entity, BulletConfig config, float damage, float spread, double sideOffset, double heightOffset, double forwardOffset) {
        // Deliberately NOT `super(entity, config, ...)` - that would resolve to
        // EntityBulletBaseMK4's own public convenience overload, which hardcodes
        // GunEntityTypes.BULLET_MK4 (see EntityBulletBaseMK4's constructor-block comment). This calls
        // the protected, EntityType-parameterized overload directly with this class's own registered
        // type instead.
        super(GunEntityTypes.BULLET_MK4CL.get(), entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateChunkLoaderTicket();
        }
    }

    private void updateChunkLoaderTicket() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        ChunkPos newPos = new ChunkPos(BlockPos.containing(this.getX(), this.getY(), this.getZ()));
        if (newPos.equals(loadedChunkPos)) return;

        if (loadedChunkPos != null) {
            serverLevel.setChunkForced(loadedChunkPos.x, loadedChunkPos.z, false);
        }
        serverLevel.setChunkForced(newPos.x, newPos.z, true);
        loadedChunkPos = newPos;
    }

    private void clearChunkLoaderTicket() {
        if (loadedChunkPos != null && level() instanceof ServerLevel serverLevel) {
            serverLevel.setChunkForced(loadedChunkPos.x, loadedChunkPos.z, false);
            loadedChunkPos = null;
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        clearChunkLoaderTicket();
    }
}
