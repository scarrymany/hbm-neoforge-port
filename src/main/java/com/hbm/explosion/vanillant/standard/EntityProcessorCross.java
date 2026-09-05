package com.hbm.explosion.vanillant.standard;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityRangeMutator;
import com.hbm.interfaces.NotableComments;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CE: {@code EntityProcessorCross} - CE's own in-file comment: "The amount of good decisions in NTM is
 * few and far between, but the VNT explosion surely is one of them." Unlike the deprecated
 * {@code EntityProcessorStandard}, this samples block density (line-of-sight-to-blast-center exposure)
 * at up to 7 points - the blast center plus {@code +-nodeDist} along all 6 axes - and takes the
 * maximum, specifically fixing vanilla's well-known "knockback vanishes right behind one thin wall"
 * bug.
 * <p>
 * CE builds those 7 sample points via {@code ForgeDirection.getOrientation(0..6)} (index 6 =
 * {@code UNKNOWN}, offset (0,0,0) - i.e. the true center). This port instead builds the 6 real
 * directions from {@link Direction#values()} plus an explicit 7th center point, rather than
 * {@code Direction.from3DDataValue(i)} for {@code i} up to 6 - vanilla's {@code Direction} only has 6
 * members (0-5), so a literal index-6 call there wraps back to index 0 (DOWN) instead of yielding a
 * center sample. (Neo Edition's own port of this class does exactly that literal call and ends up
 * sampling DOWN twice instead of ever sampling the true center - not followed here, per this port's
 * ground rule of consulting Neo Edition for API shape only, never behavior; this port's own 6-plus-
 * center construction is the one that actually reproduces CE's 7-point sample faithfully.)
 * <p>
 * See {@link EntityProcessorStandard}'s javadoc for the other API-shape notes shared with this class
 * ({@code getSeenPercent}, the knockback-resistance attribute, {@code hurtMarked}, the explosion damage
 * source).
 */
@NotableComments
public class EntityProcessorCross implements IEntityProcessor {

    protected double nodeDist = 2D;
    protected IEntityRangeMutator range;
    protected ICustomDamageHandler damage;
    protected double knockbackMult = 1D;
    protected boolean allowSelfDamage = false;

    public EntityProcessorCross() {
        this(0);
    }

    public EntityProcessorCross(double nodeDist) {
        this.nodeDist = nodeDist;
    }

    public EntityProcessorCross setAllowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }

    public EntityProcessorCross setKnockback(double mult) {
        this.knockbackMult = mult;
        return this;
    }

    /** Exact CE {@code EntityProcessorCross.java:56-59}. */
    public static boolean shouldDealKnockback(Entity entity) {
        if (entity instanceof EntityBulletBaseMK4) return false;
        if (entity instanceof EntityGrenadeUniversal) return false;
        return true;
    }

    @Override
    public HashMap<Player, Vec3> process(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {

        HashMap<Player, Vec3> affectedPlayers = new HashMap<>();

        size *= 2.0F;

        if (range != null) {
            size = range.mutateRange(explosion, size);
        }

        double minX = x - (double) size - 1.0D;
        double maxX = x + (double) size + 1.0D;
        double minY = y - (double) size - 1.0D;
        double maxY = y + (double) size + 1.0D;
        double minZ = z - (double) size - 1.0D;
        double maxZ = z + (double) size + 1.0D;

        List<Entity> list = level.getEntities(allowSelfDamage ? null : explosion.exploder, new AABB(minX, minY, minZ, maxX, maxY, maxZ));

        EventHooks.onExplosionDetonate(level, explosion.compat, list, size);

        Vec3[] nodes;

        if (this.nodeDist > 0) {
            Direction[] directions = Direction.values();
            nodes = new Vec3[directions.length + 1];
            for (int i = 0; i < directions.length; i++) {
                Direction dir = directions[i];
                nodes[i] = new Vec3(x + dir.getStepX() * nodeDist, y + dir.getStepY() * nodeDist, z + dir.getStepZ() * nodeDist);
            }
            nodes[directions.length] = new Vec3(x, y, z);
        } else {
            nodes = new Vec3[]{new Vec3(x, y, z)};
        }

        HashMap<Entity, Float> damageMap = new HashMap<>();

        for (Entity entity : list) {

            if (entity.ignoreExplosion(explosion.compat)) continue;

            AABB box = entity.getBoundingBox();
            double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
            double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
            double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
            double distanceScaled = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / size;

            if (distanceScaled <= 1.0D) {

                double deltaX = entity.getX() - x;
                double deltaY = entity.getY() + entity.getEyeHeight() - y;
                double deltaZ = entity.getZ() - z;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance != 0.0D) {

                    deltaX /= distance;
                    deltaY /= distance;
                    deltaZ /= distance;

                    double density = 0;
                    for (Vec3 vec : nodes) {
                        double d = Explosion.getSeenPercent(vec, entity);
                        if (d > density) density = d;
                    }

                    double knockback = (1.0D - distanceScaled) * density;

                    float dmg = calculateDamage(distanceScaled, density, knockback, size);
                    if (!damageMap.containsKey(entity) || damageMap.get(entity) < dmg) damageMap.put(entity, dmg);

                    double enchKnockback = knockback;
                    if (entity instanceof LivingEntity livingEntity) {
                        enchKnockback = knockback * (1.0D - livingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                    }

                    // CE: the shouldDealKnockback gate only guards the actual server-side motion
                    // impulse (bullets/grenades caught in another explosion shouldn't get flung); the
                    // affectedPlayers map below is still populated unconditionally, using the
                    // un-attenuated `knockback` value rather than `enchKnockback` - both preserved
                    // faithfully even though neither distinction is currently observable (a Player is
                    // never a bullet/grenade entity either way).
                    if (shouldDealKnockback(entity)) {
                        entity.setDeltaMovement(entity.getDeltaMovement().add(
                                deltaX * enchKnockback * knockbackMult, deltaY * enchKnockback * knockbackMult, deltaZ * enchKnockback * knockbackMult));
                    }

                    if (entity instanceof Player player) {
                        player.hurtMarked = true;
                        affectedPlayers.put(player, new Vec3(
                                deltaX * knockback * knockbackMult, deltaY * knockback * knockbackMult, deltaZ * knockback * knockbackMult));
                    }
                }
            }
        }

        for (Map.Entry<Entity, Float> entry : damageMap.entrySet()) {

            Entity entity = entry.getKey();
            attackEntity(entity, explosion, entry.getValue());

            if (damage != null) {
                AABB box = entity.getBoundingBox();
                double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
                double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
                double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
                double distanceScaled = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / size;
                damage.handleAttack(explosion, entity, distanceScaled);
            }
        }

        return affectedPlayers;
    }

    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        entity.hurt(setExplosionSource(entity.level(), source.compat), amount);
    }

    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D));
    }

    public static DamageSource setExplosionSource(Level level, Explosion explosion) {
        return level.damageSources().explosion(explosion);
    }

    public EntityProcessorCross withRangeMod(float mod) {
        range = (exp, r) -> r * mod;
        return this;
    }

    public EntityProcessorCross withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }
}
