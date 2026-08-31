package com.hbm.entity.mob;

import com.hbm.interfaces.IRadiationImmune;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * CE: {@code com.hbm.entity.mob.EntityGlowingOne} (113 lines) — rad-immune zombie, 250 HP,
 * area radiate + heal nearby zombies, convert-in-radius helper.
 */
public class EntityGlowingOne extends Zombie implements IRadiationImmune {

    public static final int EFFECT_RADIUS = 16;

    public EntityGlowingOne(EntityType<? extends EntityGlowingOne> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D);
    }

    @Override
    public void aiStep() {
        ContaminationUtil.radiate(this.level(), this.getX(), this.getY(), this.getZ(), 16, 50);
        AABB box = this.getBoundingBox().inflate(EFFECT_RADIUS);
        List<Zombie> zombies = this.level().getEntitiesOfClass(Zombie.class, box);
        for (Zombie e : zombies) {
            double dx = e.getX() - this.getX();
            double dy = (e.getY() + e.getEyeHeight()) - this.getY();
            double dz = e.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < EFFECT_RADIUS) {
                e.heal((float) (0.02 * (EFFECT_RADIUS - len)));
            }
        }
        super.aiStep();
    }

    public static void convertInRadiusToGlow(ServerLevel world, double x, double y, double z, double radius) {
        AABB box = new AABB(x, y, z, x, y, z).inflate(radius);
        for (Zombie e : world.getEntitiesOfClass(Zombie.class, box)) {
            if (e instanceof EntityGlowingOne) continue;
            double dx = e.getX() - x;
            double dy = (e.getY() + e.getEyeHeight()) - y;
            double dz = e.getZ() - z;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) < radius) {
                convertToGlow(world, e);
            }
        }
    }

    public static void convertToGlow(Level world, Zombie zombie) {
        if (zombie instanceof EntityGlowingOne) return;
        EntityGlowingOne glowing = new EntityGlowingOne(Phase9MobEntityTypes.GLOWING_ONE.get(), world);
        glowing.setBaby(zombie.isBaby());
        glowing.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(), zombie.getYRot(), zombie.getXRot());
        if (zombie.isAlive() && !world.isClientSide) {
            world.addFreshEntity(glowing);
        }
        zombie.discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 200;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("hbm", "cap_rad"))));
    }
}
