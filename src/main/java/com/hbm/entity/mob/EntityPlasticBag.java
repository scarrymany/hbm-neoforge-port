package com.hbm.entity.mob;

import com.hbm.items.tool.ToolItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * CE: {@code com.hbm.entity.mob.EntityPlasticBag} (147 lines). Water-bob trash; dies into
 * {@code plastic_bag}. Buoyant item entity not ported — vanilla drop.
 */
public class EntityPlasticBag extends WaterAnimal {

    public float rotation;
    public float prevRotation;
    private float randomMotionSpeed;
    private float rotationVelocity;
    private float randomMotionVecX;
    private float randomMotionVecY;
    private float randomMotionVecZ;

    public EntityPlasticBag(EntityType<? extends EntityPlasticBag> type, Level level) {
        super(type, level);
        this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.spawnAtLocation(new ItemStack(ToolItems.PLASTIC_BAG.get()));
            this.discard();
        }
        return true;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean isInWater() {
        return this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER)
                || this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.prevRotation = this.rotation;
        this.rotation += this.rotationVelocity;
        if (this.rotation > ((float) Math.PI * 2F)) {
            this.rotation -= ((float) Math.PI * 2F);
            if (this.random.nextInt(10) == 0) {
                this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
            }
        }

        if (this.isInWater()) {
            if (this.rotation < (float) Math.PI) {
                float f = this.rotation / (float) Math.PI;
                if (f > 0.75D) {
                    this.randomMotionSpeed = 0.1F;
                }
            } else {
                this.randomMotionSpeed *= 0.999F;
            }
            if (!this.level().isClientSide) {
                this.setDeltaMovement(
                        this.randomMotionVecX * this.randomMotionSpeed,
                        this.randomMotionVecY * this.randomMotionSpeed,
                        this.randomMotionVecZ * this.randomMotionSpeed);
            }
            Vec3 mot = this.getDeltaMovement();
            float f = Mth.sqrt((float) (mot.x * mot.x + mot.z * mot.z));
            this.yBodyRot += (float) ((-((float) Mth.atan2(mot.x, mot.z)) * 180.0F / (float) Math.PI - this.yBodyRot) * 0.1F);
            this.setYRot(this.yBodyRot);
            this.setXRot((float) (Mth.atan2(mot.y, f) * 180.0D / Math.PI));
        } else if (!this.level().isClientSide) {
            Vec3 mot = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, (mot.y - 0.08D) * 0.98D, 0.0D);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    protected void customServerAiStep() {
        ++this.noActionTime;
        if (this.noActionTime > 100) {
            this.randomMotionVecX = this.randomMotionVecY = this.randomMotionVecZ = 0.0F;
        } else if (this.random.nextInt(50) == 0 || !this.isInWater()
                || (this.randomMotionVecX == 0.0F && this.randomMotionVecY == 0.0F && this.randomMotionVecZ == 0.0F)) {
            float f = this.random.nextFloat() * (float) Math.PI * 2.0F;
            this.randomMotionVecX = Mth.cos(f) * 0.2F;
            this.randomMotionVecY = -0.1F + this.random.nextFloat() * 0.2F;
            this.randomMotionVecZ = Mth.sin(f) * 0.2F;
        }
    }
}
