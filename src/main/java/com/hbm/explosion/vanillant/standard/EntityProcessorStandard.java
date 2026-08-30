package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityRangeMutator;
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

/**
 * CE: {@code EntityProcessorStandard} - CE marks this {@code @Deprecated} itself ("an inferior version
 * to the cross processors, so there is no actual reason to ever use this one"), preserved for
 * completeness/parity with any CE caller that still constructs it directly.
 * <p>
 * API-shape notes (confirmed against Neo Edition's real, compiling 1.21.1 port of this same class,
 * cross-referenced for shape only - CE remains the source of truth for every number/behavior below):
 * <ul>
 *     <li>{@code World#getBlockDensity(Vec3d, AxisAlignedBB)} -&gt; the static
 *     {@code Explosion.getSeenPercent(Vec3, Entity)} (vanilla renamed and relocated this method; the
 *     underlying "how much of this entity's box is visually exposed to the blast center" ray-sampling
 *     algorithm it wraps is unchanged).</li>
 *     <li>CE's enchantment-based {@code EnchantmentProtection.getBlastDamageReduction(...)} blast
 *     protection has been replaced engine-side by the {@code Attributes.EXPLOSION_KNOCKBACK_RESISTANCE}
 *     attribute (a real vanilla addition, not a CE choice) - used here in its place.</li>
 *     <li>{@code EntityPlayer#motionX/Y/Z +=} becomes {@code Entity#setDeltaMovement}; explicitly
 *     setting {@code player.hurtMarked = true} is what actually triggers modern vanilla's own
 *     server-&gt;client velocity resync for the affected player (see {@code PlayerProcessorStandard}'s
 *     javadoc for why this makes CE's own knockback packet no longer necessary here).</li>
 *     <li>{@code DamageSource.causeExplosionDamage(explosion.compat)} -&gt;
 *     {@code level.damageSources().explosion(explosion.compat)}, vanilla's own modern
 *     explosion-flavored damage source family - no CE-specific {@code DamageType} needed for this
 *     generic role.</li>
 * </ul>
 */
@Deprecated
public class EntityProcessorStandard implements IEntityProcessor {

    protected IEntityRangeMutator range;
    protected ICustomDamageHandler damage;
    protected boolean allowSelfDamage = false;

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
        Vec3 vec3 = new Vec3(x, y, z);

        for (Entity entity : list) {

            if (entity.ignoreExplosion(explosion.compat)) continue;

            // CE: entity.getDistance(x, y, z) / size - a plain (non-squared) Euclidean distance.
            // Modern Entity has no direct non-squared x/y/z overload, only distanceToSqr(x, y, z); a
            // literal `distanceToSqr(...) / size` (as Neo Edition's own port of this class uses) would
            // silently change the falloff shape from linear to quadratic, so the sqrt is kept here.
            double distanceScaled = Math.sqrt(entity.distanceToSqr(x, y, z)) / size;

            if (distanceScaled <= 1.0D) {

                double deltaX = entity.getX() - x;
                double deltaY = entity.getEyeY() - y;
                double deltaZ = entity.getZ() - z;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance != 0.0D) {

                    deltaX /= distance;
                    deltaY /= distance;
                    deltaZ /= distance;

                    double density = Explosion.getSeenPercent(vec3, entity);
                    double knockback = (1.0D - distanceScaled) * density;

                    entity.hurt(setExplosionSource(level, explosion.compat), (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D)));

                    double enchKnockback = knockback;
                    if (entity instanceof LivingEntity livingEntity) {
                        enchKnockback = knockback * (1.0D - livingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                    }

                    // CE: the applied motion uses the enchant/attribute-reduced enchKnockback, but the
                    // affectedPlayers map (fed to IPlayerProcessor) is recorded with the un-reduced
                    // knockback - both preserved faithfully, not unified into one value.
                    entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * enchKnockback, deltaY * enchKnockback, deltaZ * enchKnockback));

                    if (entity instanceof Player player) {
                        player.hurtMarked = true;
                        affectedPlayers.put(player, new Vec3(deltaX * knockback, deltaY * knockback, deltaZ * knockback));
                    }

                    if (damage != null) {
                        damage.handleAttack(explosion, entity, distanceScaled);
                    }
                }
            }
        }

        return affectedPlayers;
    }

    public static DamageSource setExplosionSource(Level level, Explosion explosion) {
        return level.damageSources().explosion(explosion);
    }

    public EntityProcessorStandard withRangeMod(float mod) {
        range = (exp, r) -> r * mod;
        return this;
    }

    public EntityProcessorStandard withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }

    public EntityProcessorStandard allowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }
}
