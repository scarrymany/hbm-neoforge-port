package com.hbm.entity.mob;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.weapon.grenade.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeLoadout;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * CE: {@code com.hbm.entity.mob.EntityFBIDrone} (73 lines) — hover drone that drops a FRAG/HE/S7
 * universal grenade when over the target.
 */
public class EntityFBIDrone extends EntityUFOBase {

    private int attackCooldown;

    public EntityFBIDrone(EntityType<? extends EntityFBIDrone> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMobAttributes.create()
                .add(Attributes.MAX_HEALTH, 35.0D);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.courseChangeCooldown > 0) {
            this.courseChangeCooldown--;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
        }

        if (!this.level().isClientSide) {
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (this.target != null && this.attackCooldown <= 0) {
                Vec3 vec = new Vec3(this.getX() - this.target.getX(), this.getY() - this.target.getY(),
                        this.getZ() - this.target.getZ());
                if (Math.abs(vec.x) < 5.0D && Math.abs(vec.z) < 5.0D && vec.y > 3.0D) {
                    this.attackCooldown = 60;
                    EntityGrenadeUniversal grenade = new EntityGrenadeUniversal(this.level(),
                            new GrenadeLoadout(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S7, null));
                    grenade.setPos(this.getX(), this.getY(), this.getZ());
                    this.level().addFreshEntity(grenade);
                }
            }
        }

        if (this.courseChangeCooldown > 0) {
            this.approachPosition(this.target == null ? 0.25D : 0.5D);
        }
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + this.random.nextInt(4);
    }

    @Override
    protected int wanderHeightOffset() {
        return 7 + this.random.nextInt(4);
    }

    /** Tiny helper so the drone doesn't pull FlyingMob.createMobAttributes from a missing method. */
    private static final class FlyingMobAttributes {
        static AttributeSupplier.Builder create() {
            return net.minecraft.world.entity.Mob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 35.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.3D)
                    .add(Attributes.FOLLOW_RANGE, 100.0D);
        }
    }
}
