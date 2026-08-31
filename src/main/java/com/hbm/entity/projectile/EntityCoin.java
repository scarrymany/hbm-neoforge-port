package com.hbm.entity.projectile;

import com.hbm.entity.GunEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.projectile.EntityCoin} (49 lines, read in full - note the real
 * package, see {@code docs/phase4/entities_orbital_and_beam_payloads.md}'s headline finding #1: this
 * is <b>not</b> a currency/trophy item, it's a live thrown projectile with real gravity/collision
 * that a beam weapon can strike mid-air to trigger a "coin flip" ricochet - see
 * {@link EntityBulletBeamBase#performHitscan()} for the relay-beam half of that mechanic).
 * <p>
 * Builds directly on this port's already-real {@link EntityThrowableInterp}/{@link
 * EntityThrowableNT}: every CE override lands on an exact, confirmed 1.21.1 hook -
 * {@link #getAirDrag()} (CE: {@code 1F}), {@link #getGravityVelocity()} (CE: {@code 0.02F}),
 * {@link #onImpact(HitResult)} (dies only on a block hit), and CE's {@code canBeCollidedWith()} onto
 * vanilla {@link #isPickable()} - the same gate {@link EntityThrowableNT#tick()} and {@link
 * EntityBulletBeamBase#performHitscan()} already use for every other entity.
 * <p>
 * <b>Deliberate simplification</b>: CE's own {@code setPosition} override rebuilds a custom AABB
 * shifted down by half a block ({@code y - 0.5} to {@code y - 0.5 + height}, i.e. the coin's visual
 * bounding box sits centered slightly below the entity's actual Y position) - a rendering/collision-
 * shape nuance with no gameplay-affecting consequence (the coin-flip beam intersection test already
 * grows whatever bounding box is registered by a fixed margin). Registered at CE's own declared
 * {@code (1F, 0.5F)} size via {@code EntityType.Builder.sized(...)} instead of re-deriving the
 * vertical-offset trick; revisit alongside Phase 5's renderer if the exact hitbox placement ever
 * turns out to matter for a specific trick shot.
 */
public class EntityCoin extends EntityThrowableInterp {

    public EntityCoin(EntityType<? extends EntityCoin> type, Level level) {
        super(type, level);
    }

    public EntityCoin(Level level) {
        this(GunEntityTypes.COIN.get(), level);
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    public double getGravityVelocity() {
        return 0.02D;
    }

    @Override
    protected void onImpact(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            this.discard();
        }
    }

    /** CE's {@code canBeCollidedWith() -> true} - the real vanilla 1.21.1 landing spot every other
     *  hit-sweep in this port's ballistics core already consults for exactly this purpose. */
    @Override
    public boolean isPickable() {
        return true;
    }

    /**
     * Matches CE's real (only) spawn call site, {@code XFactoryAccelerator.LAMBDA_NI4NI_SECONDARY_
     * PRESS} (read in full): position at eye height minus the coin's own height minus 0.125, motion
     * from the thrower's look vector scaled 0.8 with +0.5 added vertical lift, yaw copied from the
     * thrower. {@code ItemGunNI4NI}'s own per-stack coin economy (count/charge NBT, upgrade checks)
     * is a separate, still-unbuilt {@code ItemGunBaseNT} subclass - out of this entity package's
     * scope per the report's Deferred scope - so nothing currently calls this yet; provided as the
     * real, faithful landing spot for whichever future pass builds that item.
     */
    public static EntityCoin throwFrom(LivingEntity thrower) {
        Level level = thrower.level();
        EntityCoin coin = new EntityCoin(level);
        coin.setOwner(thrower);

        Vec3 look = thrower.getLookAngle().scale(0.8D);
        coin.setPos(thrower.getX(), thrower.getY() + thrower.getEyeHeight() - coin.getBbHeight() - 0.125D, thrower.getZ());
        coin.setDeltaMovement(look.x, look.y + 0.5D, look.z);
        coin.setYRot(thrower.getYRot());

        level.addFreshEntity(coin);
        return coin;
    }
}
