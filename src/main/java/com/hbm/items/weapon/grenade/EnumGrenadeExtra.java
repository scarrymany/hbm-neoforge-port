package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra} (4 values -
 * the only optional component; a grenade may carry none). See {@link EnumGrenadeFuze}'s javadoc for
 * why this class's behavior methods are private statics referenced by method handle rather than
 * CE's own "public static final lambda field on the outer item class" shape.
 */
public enum EnumGrenadeExtra implements StringRepresentable {

    /** Sticky bombs - embeds in the hit block instead of exploding/bouncing. */
    GLUE(null, EnumGrenadeExtra::onImpactGlue, null),
    /** 10-block {@code LivingEntity} proximity trigger, checked every 3rd tick starting at tick 10. */
    PROXY_FUZE(EnumGrenadeExtra::updateProxy, null, null),
    /** 25 extra kinetic fragmentation pellets on top of the filling's own explosion. */
    FRAG_SLEEVE(null, null, EnumGrenadeExtra::explodeFragSleeve),
    /** "The big one" (CE's own in-code label) - spawns 3 re-fuzed (3s) child grenades 120 degrees apart. */
    TRIPLEX(null, null, EnumGrenadeExtra::explodeTriplex);

    public static final EnumGrenadeExtra[] VALUES = values();

    public static final Codec<EnumGrenadeExtra> CODEC = StringRepresentable.fromEnum(EnumGrenadeExtra::values);

    public final Consumer<EntityGrenadeUniversal> updateTick;
    public final BiConsumer<EntityGrenadeUniversal, HitResult> onImpact;
    public final Consumer<EntityGrenadeUniversal> onExplode;

    EnumGrenadeExtra(Consumer<EntityGrenadeUniversal> updateTick,
                      BiConsumer<EntityGrenadeUniversal, HitResult> onImpact,
                      Consumer<EntityGrenadeUniversal> onExplode) {
        this.updateTick = updateTick;
        this.onImpact = onImpact;
        this.onExplode = onExplode;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static void onImpactGlue(EntityGrenadeUniversal grenade, HitResult mop) {
        if (mop instanceof BlockHitResult bhr) {
            Vec3 hit = bhr.getLocation();
            grenade.setPos(hit.x, hit.y, hit.z);
            Direction side = bhr.getDirection();
            grenade.getStuck(bhr.getBlockPos(), side.ordinal());
        }
    }

    private static void updateProxy(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() >= 10 && grenade.getTimer() % 3 == 0) {
            AABB box = new AABB(grenade.getX(), grenade.getY(), grenade.getZ(), grenade.getX(), grenade.getY(), grenade.getZ()).inflate(10, 10, 10);
            Level level = grenade.level();
            List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, box);
            for (LivingEntity e : living) {
                if (e == grenade.getThrower()) continue;
                if (e.distanceTo(grenade) <= 10) {
                    grenade.explode();
                    return;
                }
            }
        }
    }

    private static void explodeFragSleeve(EntityGrenadeUniversal grenade) {
        GrenadeFillingActions.standardFragmentation(grenade, 25);
    }

    private static void explodeTriplex(EntityGrenadeUniversal grenade) {
        GrenadeLoadout frag = new GrenadeLoadout(grenade.getShell(), grenade.getFilling(), EnumGrenadeFuze.S3, null);

        Level level = grenade.level();
        float baseYaw = level.getRandom().nextFloat() * 360F;

        for (int i = 0; i < 3; i++) {
            EntityGrenadeUniversal triplet = new EntityGrenadeUniversal(level, frag).setTrail(EntityGrenadeUniversal.TRAIL_TRIPLET);
            triplet.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
            triplet.setOwnerEntity(grenade.getOwner());

            float yaw = baseYaw + i * 120F;
            double vx = 0.25D * Math.sin(Math.toRadians(yaw));
            double vz = 0.25D * Math.cos(Math.toRadians(yaw));
            triplet.setDeltaMovement(vx, 0.75D, vz);

            level.addFreshEntity(triplet);
        }
    }
}
