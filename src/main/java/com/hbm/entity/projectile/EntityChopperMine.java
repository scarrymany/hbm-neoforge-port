package com.hbm.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Direct port of CE's {@code com.hbm.entity.projectile.EntityChopperMine} (145 lines, {@code extends
 * Entity implements IProjectile}, read in full per this task's explicit instruction - flagged unread
 * by both prior Phase 4 research reports). {@link EntityHunterChopper}'s only dropped hazard: a
 * gravity-falling proximity mine that detonates on player contact, on landing, or after 100 ticks.
 * Small and self-contained (no inventory, no GUI, no cross-package dependency beyond the shooter
 * reference) - ported alongside the chopper itself per this task's own instruction rather than left as
 * a gap.
 * <p>
 * <b>{@code CompatibilityConfig.isWarDim} gate dropped</b>: CE only detonates
 * ({@code world.createExplosion}) when {@code isWarDim(world)} is true. That per-dimension config table
 * is explicitly not ported in this port (1.12 numeric dimension ids have no 1.21 equivalent - see
 * {@code docs/phase4/entities_vehicles_aircraft.md}'s Deferred scope and this port's own
 * {@code HbmPotionEffects#isWarDim}, which stubs the identical CE mechanic to always-{@code true} for
 * the same reason: CE's real default is "every dimension is a war dimension until an operator opts one
 * out"). This class always detonates, matching that same precedent rather than silently going inert.
 * <p>
 * <b>Simplified target detection</b>: CE's own raytrace-and-avoid-shooter logic (a manual AABB
 * intercept sweep over every nearby entity) is replaced with a straightforward block-clip plus a
 * nearby-{@link Player}-excluding-the-shooter scan - same practical trigger conditions (player contact,
 * landing on a block, or timeout), simpler code, since this port has no client-side rendering of this
 * entity yet for the exact intercept point to matter visually.
 */
public class EntityChopperMine extends Entity {

    private int timer = 0;
    @Nullable
    private Entity shooter;

    public EntityChopperMine(EntityType<? extends EntityChopperMine> type, Level level) {
        super(type, level);
    }

    /** CE: {@code EntityChopperMine(World, double x, double y, double z, double moX, double moY, double moZ, Entity shooter)}. */
    public EntityChopperMine(EntityType<? extends EntityChopperMine> type, Level level, double x, double y, double z, double mx, double my, double mz, @Nullable Entity shooter) {
        this(type, level);
        this.setPos(x, y, z);
        this.setDeltaMovement(mx, my, mz);
        this.shooter = shooter;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced state - this entity has no client rendering yet (Phase 5).
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        Vec3 from = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 to = from.add(motion);

        boolean shouldDetonate = false;

        HitResult blockHit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            shouldDetonate = true;
        }

        AABB scanBox = this.getBoundingBox().inflate(Math.abs(motion.x), Math.abs(motion.y), Math.abs(motion.z)).inflate(1.0D);
        for (Player player : this.level().getEntitiesOfClass(Player.class, scanBox)) {
            if (player != this.shooter) {
                shouldDetonate = true;
                break;
            }
        }

        BlockPos here = this.blockPosition();
        if (this.timer >= 100 || !this.level().getBlockState(here).isAir()) {
            shouldDetonate = true;
        }

        if (shouldDetonate) {
            this.level().explode(this.shooter != null ? this.shooter : this, this.getX(), this.getY(), this.getZ(), 5F, Level.ExplosionInteraction.MOB);
            this.discard();
            return;
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), com.hbm.lib.HBMSoundHandler.nullMine.get(), SoundSource.HOSTILE, 10.0F, 1.0F);

        if (motion.y > -0.85D) {
            motion = motion.add(0, -0.05D, 0);
        }
        motion = new Vec3(motion.x * 0.9D, motion.y, motion.z * 0.9D);
        this.setDeltaMovement(motion);
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        this.timer++;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.timer = tag.getInt("timer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("timer", this.timer);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
