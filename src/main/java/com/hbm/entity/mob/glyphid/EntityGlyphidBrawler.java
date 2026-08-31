package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** CE: {@code EntityGlyphidBrawler} (141 lines) — leap at target. */
public class EntityGlyphidBrawler extends EntityGlyphid {

    public int timer;

    public EntityGlyphidBrawler(EntityType<? extends EntityGlyphidBrawler> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getBrawler());
    }

    @Override
    public void tick() {
        super.tick();
        Entity e = this.getTarget();
        if (e != null && this.isAlive() && --timer <= 0) {
            leap();
            timer = 80 + this.random.nextInt(30);
        }
    }

    public void leap() {
        if (this.level().isClientSide) return;
        LivingEntity target = this.getTarget();
        if (!(target instanceof LivingEntity) || this.distanceTo(target) >= 20) return;
        Vec3 delta = new Vec3(target.getX() - this.getX(),
                (target.getY() + target.getBbHeight() / 2) - (this.getY() + 1),
                target.getZ() - this.getZ());
        if (delta.length() < 3) return;
        double v0 = 1.5;
        Vec3 n = delta.normalize().scale(v0);
        this.setDeltaMovement(n.x, Math.max(0.4D, n.y), n.z);
        this.setYRot((float) (Mth.atan2(n.x, n.z) * 180.0D / Math.PI));
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_brawler.png");
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrawler;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL) && amount <= 10) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }
}
