package com.hbm.entity.missile;

import com.hbm.damage.ModDamageTypes;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * CE {@code com.hbm.entity.missile.EntitySoyuz} (~209 lines) —
 * {@code @AutoRegister(name = "entity_soyuz", trackingRange = 1000)}.
 * Satellite orbit / achievements stubbed. Mode 1 spawns {@link EntitySoyuzCapsule}.
 */
public class EntitySoyuz extends Entity {

    public static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(EntitySoyuz.class, EntityDataSerializers.INT);

    private double acceleration;
    public int mode;
    public int targetX;
    public int targetZ;
    private boolean memed;
    private final ItemStack[] payload = new ItemStack[18];

    public EntitySoyuz(EntityType<? extends EntitySoyuz> type, Level level) {
        super(type, level);
        java.util.Arrays.fill(payload, ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SKIN, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getDeltaMovement().y < 2.0D) {
            acceleration += 0.00025D;
            this.setDeltaMovement(this.getDeltaMovement().add(0, acceleration, 0));
        }
        this.setPos(this.getX() + this.getDeltaMovement().x,
                this.getY() + this.getDeltaMovement().y,
                this.getZ() + this.getDeltaMovement().z);

        if (!this.level().isClientSide) {
            AABB box = new AABB(this.getX() - 5, this.getY() - 15, this.getZ() - 5,
                    this.getX() + 5, this.getY(), this.getZ() + 5);
            for (Entity e : this.level().getEntities(this, box)) {
                e.igniteForSeconds(15);
                e.hurt(this.damageSources().source(ModDamageTypes.EXHAUST, this), 100.0F);
                if (e instanceof Player && !memed) {
                    memed = true;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            HBMSoundHandler.soyuzed.get(), SoundSource.NEUTRAL, 100, 1.0F);
                }
            }
        }
        if (this.getY() > 600) {
            deployPayload();
        }
    }

    private void deployPayload() {
        if (mode == 1) {
            EntitySoyuzCapsule capsule = new EntitySoyuzCapsule(this.level());
            capsule.setPayload(this.payload);
            capsule.soyuz = this.getSkin();
            capsule.setPos(targetX + 0.5, 600, targetZ + 0.5);
            this.level().addFreshEntity(capsule);
        } else if (!payload[0].isEmpty() && !this.level().isClientSide) {
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), payload[0].copy()));
        }
        this.discard();
    }

    public void setSat(ItemStack stack) {
        this.payload[0] = stack;
    }

    public void setPayload(java.util.List<ItemStack> items) {
        for (int i = 0; i < items.size() && i < payload.length; i++) {
            payload[i] = items.get(i);
        }
    }

    public void setSkin(int i) {
        this.entityData.set(SKIN, i);
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D * 500000D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.setSkin(nbt.getInt("skin"));
        targetX = nbt.getInt("targetX");
        targetZ = nbt.getInt("targetZ");
        mode = nbt.getInt("mode");
        ListTag list = nbt.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            byte b0 = slot.getByte("slot");
            if (b0 >= 0 && b0 < payload.length) {
                payload[b0] = ItemStack.parseOptional(this.registryAccess(), slot);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("skin", this.getSkin());
        nbt.putInt("targetX", targetX);
        nbt.putInt("targetZ", targetZ);
        nbt.putInt("mode", mode);
        ListTag list = new ListTag();
        for (int i = 0; i < payload.length; i++) {
            if (!payload[i].isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("slot", (byte) i);
                list.add(payload[i].save(this.registryAccess(), slot));
            }
        }
        nbt.put("items", list);
    }
}
