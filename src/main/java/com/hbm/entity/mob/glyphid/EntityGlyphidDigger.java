package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.EntityRubble;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** CE: {@code EntityGlyphidDigger} (169 lines) — ground slam throws rubble. */
public class EntityGlyphidDigger extends EntityGlyphid {

    public int timer;

    public EntityGlyphidDigger(EntityType<? extends EntityGlyphidDigger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getDigger());
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_digger.png");
    }

    @Override
    public double getGlyphidScale() {
        return 1.3D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsDigger;
    }

    @Override
    public void tick() {
        super.tick();
        Entity e = this.getTarget();
        if (e != null && this.isAlive() && --timer <= 0) {
            groundSlam();
            timer = 120;
        }
    }

    public void groundSlam() {
        if (this.level().isClientSide) return;
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceTo(target) >= 30) return;
        Vec3 look = this.getLookAngle();
        BlockPos origin = this.blockPosition();
        for (int i = 1; i <= 6; i++) {
            BlockPos p = origin.offset((int) Math.round(look.x * i), 0, (int) Math.round(look.z * i));
            BlockState state = this.level().getBlockState(p);
            if (state.isAir() || state.getDestroySpeed(this.level(), p) < 0) continue;
            if (state.getExplosionResistance(this.level(), p, null) >= Blocks.OBSIDIAN.getExplosionResistance()) continue;
            EntityRubble rubble = new EntityRubble(this.level(), p.getX() + 0.5F, p.getY() + 2, p.getZ() + 0.5F);
            rubble.setBlockState(state);
            Vec3 fire = new Vec3(target.getX() - p.getX(), target.getY() - p.getY(), target.getZ() - p.getZ()).normalize().scale(1.2);
            rubble.shoot(fire.x, fire.y, fire.z, 1.2F, this.random.nextFloat());
            this.level().addFreshEntity(rubble);
            this.level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }
}
