package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.EntityAcidBomb;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * CE {@code EntityGlyphidBombardier} (122 lines) —
 * {@code @AutoRegister(name = "entity_glyphid_bombardier")} at line 15.
 * Ballistic acid-bomb volley: {@code onUpdate} CE lines 45-105.
 */
public class EntityGlyphidBombardier extends EntityGlyphid {

    protected Entity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;

    public EntityGlyphidBombardier(EntityType<? extends EntityGlyphidBombardier> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        StatBundle s = GlyphidStats.getStats().getBombardier();
        return MonsterAttrs.of(s);
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_bombardier.png");
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBombardier;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity e = this.getTarget();
        if (this.level().isClientSide || !(e instanceof LivingEntity)) {
            return;
        }

        if (this.tickCount % 20 == 0) {
            this.lastTarget = e;
            this.lastX = e.getX();
            this.lastY = e.getY();
            this.lastZ = e.getZ();
        }

        if (this.tickCount % 60 != 1) {
            return;
        }

        boolean topAttack = false;
        double velX = e.getX() - lastX;
        double velY = e.getY() - lastY;
        double velZ = e.getZ() - lastZ;

        if (this.lastTarget != e || Math.sqrt(velX * velX + velY * velY + velZ * velZ) > 30) {
            velX = velY = velZ = 0;
        }

        if (this.distanceTo(e) > 20) {
            topAttack = true;
        }

        int prediction = topAttack ? 60 : 20;
        double dx = e.getX() - this.getX() + velX * prediction;
        double dy = (e.getY() + e.getBbHeight() / 2) - (this.getY() + 1) + velY * prediction;
        double dz = e.getZ() - this.getZ() + velZ * prediction;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 3) {
            return;
        }
        double targetYaw = -Math.atan2(dx, dz);

        double x = Math.sqrt(dx * dx + dz * dz);
        double y = dy;
        double v0 = getV0();
        double v02 = v0 * v0;
        double g = 0.04D;
        double upperLower = topAttack ? 1 : -1;
        double disc = v02 * v02 - g * (g * x * x + 2 * y * v02);
        if (disc < 0) {
            return;
        }
        double targetPitch = Math.atan((v02 + Math.sqrt(disc) * upperLower) / (g * x));
        if (Double.isNaN(targetPitch)) {
            return;
        }

        // CE Vec3NT: start (v0,0,0), rotateRoll(-pitch), rotateYaw(-(yaw + π/2))
        double[] fire = {v0, 0, 0};
        rotateRoll(fire, -targetPitch);
        rotateYaw(fire, -(targetYaw + Math.PI * 0.5));

        for (int i = 0; i < getBombCount(); i++) {
            EntityAcidBomb bomb = new EntityAcidBomb(this.level(), this.getX(), this.getY() + 1, this.getZ());
            bomb.setThrower(this);
            bomb.shoot(fire[0], fire[1], fire[2], (float) v0, i * getSpreadMult());
            bomb.damage = getBombDamage();
            this.level().addFreshEntity(bomb);
        }
        this.swing(InteractionHand.MAIN_HAND);
    }

    public float getBombDamage() {
        return 5F;
    }

    public int getBombCount() {
        return 5;
    }

    public float getSpreadMult() {
        return 1F;
    }

    public double getV0() {
        return 1D;
    }

    /** CE {@code MutableVec3d.rotateRollSelf} — rotate around Z. */
    private static void rotateRoll(double[] v, double roll) {
        double c = Math.cos(roll);
        double s = Math.sin(roll);
        double nx = v[0] * c + v[1] * s;
        double ny = v[1] * c - v[0] * s;
        v[0] = nx;
        v[1] = ny;
    }

    /** CE {@code MutableVec3d.rotateYawSelf} — rotate around Y. */
    private static void rotateYaw(double[] v, double yaw) {
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        double nx = v[0] * c + v[2] * s;
        double nz = v[2] * c - v[0] * s;
        v[0] = nx;
        v[2] = nz;
    }

    static final class MonsterAttrs {
        static AttributeSupplier.Builder of(StatBundle s) {
            return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                    .add(Attributes.MAX_HEALTH, s.health())
                    .add(Attributes.MOVEMENT_SPEED, s.speed())
                    .add(Attributes.ATTACK_DAMAGE, s.damage())
                    .add(Attributes.FOLLOW_RANGE, 16.0D);
        }
    }
}
