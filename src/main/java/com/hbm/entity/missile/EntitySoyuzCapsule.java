package com.hbm.entity.missile;

import com.hbm.entity.projectile.Phase9TailEntityTypes;
import com.hbm.items.special.SpecialItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * CE {@code com.hbm.entity.missile.EntitySoyuzCapsule} (110 lines) —
 * {@code @AutoRegister(name = "entity_soyuz_capsule", trackingRange = 1000)}.
 * {@code ModBlocks.soyuz_capsule} is not in this port — drops payload + flattened soyuz item instead.
 */
public class EntitySoyuzCapsule extends Entity {

    public int soyuz;
    public ItemStack[] payload = new ItemStack[18];

    public EntitySoyuzCapsule(EntityType<? extends EntitySoyuzCapsule> type, Level level) {
        super(type, level);
        java.util.Arrays.fill(payload, ItemStack.EMPTY);
    }

    public EntitySoyuzCapsule(Level level) {
        this(Phase9TailEntityTypes.SOYUZ_CAPSULE.get(), level);
    }

    public void setPayload(ItemStack[] src) {
        System.arraycopy(src, 0, this.payload, 0, Math.min(src.length, this.payload.length));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        Vec3 mot = this.getDeltaMovement();
        if (mot.y > -0.2) {
            mot = mot.subtract(0, 0.02, 0);
            this.setDeltaMovement(mot);
        }
        double y = this.getY();
        if (y > 600) {
            y = 600;
        }
        this.setPos(this.getX() + mot.x, y + mot.y, this.getZ() + mot.z);

        BlockPos pos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        if (!this.level().getBlockState(pos).isAir()) {
            this.discard();
            if (!this.level().isClientSide) {
                for (ItemStack stack : payload) {
                    if (!stack.isEmpty()) {
                        this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY() + 1, this.getZ(), stack.copy()));
                    }
                }
                ItemStack soyuzItem = switch (soyuz) {
                    case 1 -> new ItemStack(SpecialItems.MISSILE_SOYUZ_LUNAR.get());
                    case 2 -> new ItemStack(SpecialItems.MISSILE_SOYUZ_POST_WAR.get());
                    default -> new ItemStack(SpecialItems.MISSILE_SOYUZ_NORMAL.get());
                };
                this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY() + 1, this.getZ(), soyuzItem));
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D * 500000D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        soyuz = nbt.getInt("soyuz");
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
        nbt.putInt("soyuz", soyuz);
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
