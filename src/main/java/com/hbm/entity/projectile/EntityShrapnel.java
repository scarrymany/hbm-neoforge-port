package com.hbm.entity.projectile;

import com.hbm.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * CE {@code com.hbm.entity.projectile.EntityShrapnel} (164 lines) —
 * {@code @AutoRegister(name = "entity_shrapnel", trackingRange = 1000)}.
 * Trail byte kept. Volcano/mud/lava block placement skipped — those CE blocks are unregistered here.
 */
public class EntityShrapnel extends EntityThrowableNT {

    public static final EntityDataAccessor<Byte> TRAIL =
            SynchedEntityData.defineId(EntityShrapnel.class, EntityDataSerializers.BYTE);

    public EntityShrapnel(EntityType<? extends EntityShrapnel> type, Level level) {
        super(type, level);
    }

    public EntityShrapnel(Level level, LivingEntity thrower) {
        super(Phase9TailEntityTypes.SHRAPNEL.get(), level, thrower);
    }

    public EntityShrapnel(Level level, double x, double y, double z) {
        super(Phase9TailEntityTypes.SHRAPNEL.get(), level, x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRAIL, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onImpact(HitResult result) {
        if (result instanceof EntityHitResult ehr) {
            ehr.getEntity().hurt(this.damageSources().source(ModDamageTypes.SHRAPNEL, this, this.getOwner()), 15.0F);
        }
        if (this.tickCount <= 5) {
            return;
        }
        this.discard();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public void setTrail(boolean enabled) {
        this.entityData.set(TRAIL, (byte) (enabled ? 1 : 0));
    }

    public void setVolcano(boolean enabled) {
        this.entityData.set(TRAIL, (byte) (enabled ? 2 : 0));
    }

    public void setWatz(boolean enabled) {
        this.entityData.set(TRAIL, (byte) (enabled ? 3 : 0));
    }

    public void setRadVolcano(boolean enabled) {
        this.entityData.set(TRAIL, (byte) (enabled ? 4 : 0));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("trail", this.entityData.get(TRAIL));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TRAIL, tag.getByte("trail"));
    }
}
