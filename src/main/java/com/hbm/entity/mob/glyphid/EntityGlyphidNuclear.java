package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.Phase9MobEntityTypes;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** CE: {@code EntityGlyphidNuclear} (181 lines) — delayed death explosion. */
public class EntityGlyphidNuclear extends EntityGlyphid {

    public int deathTicks;

    public EntityGlyphidNuclear(EntityType<? extends EntityGlyphidNuclear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getNuclear());
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_nuclear.png");
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsNuclear;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void communicate(byte task) {
        AABB bb = this.getBoundingBox().inflate(4);
        for (Entity e : this.level().getEntities(this, bb)) {
            if (e instanceof EntityGlyphidScout scout && scout.getCurrentTask() != task) {
                scout.setCurrentTask(task);
            }
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    public boolean doesInfectedSpawnMaggots() {
        return false;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTicks;
        if (this.deathTicks == 1) {
            this.communicate(TASK_INITIATE_RETREAT);
        }
        if (this.deathTicks == 90) {
            AABB bb = this.getBoundingBox().inflate(8);
            for (Entity e : this.level().getEntities(this, bb)) {
                if (e instanceof EntityGlyphid bug) {
                    bug.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 6));
                    bug.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 15 * 20, 1));
                }
            }
        }
        if (this.deathTicks == 100) {
            if (!this.level().isClientSide) {
                if (this.entityData.get(SUBTYPE) == TYPE_INFECTED) {
                    int j = 15 + this.random.nextInt(6);
                    for (int k = 0; k < j; ++k) {
                        float f = ((k % 2) - 0.5F) * 0.5F;
                        float f1 = ((k / 2) - 0.5F) * 0.5F;
                        EntityParasiteMaggot maggot = new EntityParasiteMaggot(Phase9MobEntityTypes.PARASITE_MAGGOT.get(), this.level());
                        maggot.moveTo(this.getX() + f, this.getY() + 0.5D, this.getZ() + f1,
                                this.random.nextFloat() * 360.0F, 0.0F);
                        maggot.setDeltaMovement(f, 0, f1);
                        this.level().addFreshEntity(maggot);
                    }
                } else {
                    ExplosionVNT vnt = new ExplosionVNT(this.level(), this.getX(), this.getY(), this.getZ(), 25, this);
                    vnt.setBlockAllocator(new BlockAllocatorStandard(24));
                    vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                    vnt.setEntityProcessor(new EntityProcessorStandard());
                    vnt.setPlayerProcessor(new PlayerProcessorStandard());
                    vnt.explode();
                }
                this.level().playSound(null, this.blockPosition(), HBMSoundHandler.mukeExplosion.get(),
                        SoundSource.HOSTILE, 15.0F, 1.0F);
            }
            this.discard();
        } else if (!this.level().isClientSide && this.deathTicks % 10 == 0) {
            this.level().playSound(null, this.blockPosition(), HBMSoundHandler.fstbmbPing.get(),
                    SoundSource.HOSTILE, 5.0F, 1.0F);
        }
    }
}
