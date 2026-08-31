package com.hbm.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * CE: {@code com.hbm.entity.mob.EntityPigeon} (238 lines). Flying AI classes
 * ({@code EntityAIStartFlying} etc.) not ported — flight toggle + motion live in
 * {@link #customServerAiStep} / {@link #aiStep}. Eat-bread = tempt + nearby bread pickup.
 */
public class EntityPigeon extends PathfinderMob implements IFlyingCreature {

    private static final EntityDataAccessor<Byte> FLYING_STATE =
            SynchedEntityData.defineId(EntityPigeon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FAT_STATE =
            SynchedEntityData.defineId(EntityPigeon.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float dest;
    public float prevDest;
    public float prevFallTime;
    public float offGroundTimer = 1.0F;

    public EntityPigeon(EntityType<? extends EntityPigeon> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING_STATE, (byte) 0);
        builder.define(FAT_STATE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TemptGoal(this, 0.4D, Ingredient.of(Items.BREAD), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.2D) {
            @Override
            public boolean canUse() {
                return getFlyingState() == IFlyingCreature.STATE_WALKING && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (amount >= this.getMaxHealth() * 2 && !this.level().isClientSide) {
            this.discard();
            for (int i = 0; i < 10; i++) {
                Vec3 vec = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).normalize();
                ItemEntity feather = new ItemEntity(this.level(),
                        this.getX() + vec.x, this.getY() + this.getBbHeight() / 2.0D + vec.y, this.getZ() + vec.z,
                        new ItemStack(Items.FEATHER));
                feather.setDeltaMovement(vec.scale(0.5D));
                this.level().addFreshEntity(feather);
            }
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {
        int featherCount = this.random.nextInt(3) + this.random.nextInt(1 + (attackedRecently ? 1 : 0));
        for (int i = 0; i < featherCount; i++) {
            this.spawnAtLocation(Items.FEATHER);
        }
        this.spawnAtLocation(this.isOnFire()
                ? (this.isFat() ? new ItemStack(Items.COOKED_CHICKEN, 3) : new ItemStack(Items.COOKED_CHICKEN))
                : (this.isFat() ? new ItemStack(Items.CHICKEN, 3) : new ItemStack(Items.CHICKEN)));
    }

    @Override
    public int getFlyingState() {
        return this.entityData.get(FLYING_STATE);
    }

    @Override
    public void setFlyingState(int state) {
        this.entityData.set(FLYING_STATE, (byte) state);
    }

    public boolean isFat() {
        return this.entityData.get(FAT_STATE) == 1;
    }

    public void setFat(boolean fat) {
        this.entityData.set(FAT_STATE, (byte) (fat ? 1 : 0));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.getFlyingState() == IFlyingCreature.STATE_FLYING) {
            int height = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                    Mth.floor(this.getX()), Mth.floor(this.getZ()));
            boolean ceil = this.getY() - height > 10;
            double my = this.random.nextGaussian() * 0.05D + (ceil ? 0.0D : 0.04D) + (this.isInWater() ? 0.2D : 0.0D);
            if (this.onGround()) {
                my = Math.abs(my) + 0.1D;
            }
            Vec3 look = this.getLookAngle();
            this.setDeltaMovement(look.x * 0.3D, my, look.z * 0.3D);
            if (this.random.nextInt(20) == 0) {
                this.setYRot(this.getYRot() + (float) (this.random.nextGaussian() * 30.0D));
            }
            if (this.random.nextInt(400) == 0) {
                this.setFlyingState(IFlyingCreature.STATE_WALKING);
            }
        } else {
            if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.8D, 1.0D));
            }
            if (this.random.nextInt(400) == 0) {
                this.setFlyingState(IFlyingCreature.STATE_FLYING);
            }
            for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D))) {
                if (item.getItem().is(Items.BREAD)) {
                    item.discard();
                    this.setFat(true);
                    break;
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.prevFallTime = this.fallTime;
        this.prevDest = this.dest;
        this.dest = (float) (this.dest + (this.onGround() ? -1 : 4) * 0.3D);
        this.dest = Mth.clamp(this.dest, 0.0F, 1.0F);
        if (!this.onGround() && this.offGroundTimer < 1.0F) {
            this.offGroundTimer = 1.0F;
        }
        this.offGroundTimer *= 0.9F;
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }
        this.fallTime += this.offGroundTimer * 2.0F;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("flying", this.entityData.get(FLYING_STATE));
        tag.putByte("fat", this.entityData.get(FAT_STATE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(FLYING_STATE, tag.getByte("flying"));
        this.entityData.set(FAT_STATE, tag.getByte("fat"));
    }
}
