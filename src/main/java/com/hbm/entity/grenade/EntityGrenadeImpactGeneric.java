package com.hbm.entity.grenade;

import com.hbm.items.weapon.ItemGenericGrenade;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.entity.grenade.EntityGrenadeImpactGeneric} (62 lines) - the
 * {@code fuse == -1} "explodes on first touch" path of {@code ItemGenericGrenade}. Per
 * {@code docs/phase3/grenades.md}: this path is currently unreachable in CE itself (grepped - no
 * registered {@code ModItems} grenade is constructed with {@code fuse == -1}; both
 * {@code stick_dynamite}/{@code stick_dynamite_fishing} use {@code fuse=3}, routing through
 * {@link EntityGrenadeBouncyGeneric} instead). Ported anyway for registry/class-layout parity, with
 * zero currently-observable behavior in this port either - see that class for how the shared item-id
 * sync deviates from CE's raw-int scheme.
 */
public class EntityGrenadeImpactGeneric extends EntityGrenadeBase implements IGenericGrenade {

    private static final EntityDataAccessor<String> DATA_GRENADE_ITEM =
            SynchedEntityData.defineId(EntityGrenadeImpactGeneric.class, EntityDataSerializers.STRING);

    public EntityGrenadeImpactGeneric(EntityType<? extends EntityGrenadeImpactGeneric> type, Level level) {
        super(type, level);
    }

    public EntityGrenadeImpactGeneric(Level level, LivingEntity thrower, InteractionHand hand) {
        super(GrenadeEntityTypes.GRENADE_IMPACT_GENERIC.get(), level, thrower, hand);
    }

    public EntityGrenadeImpactGeneric setType(ItemGenericGrenade grenade) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(grenade);
        this.getEntityData().set(DATA_GRENADE_ITEM, id == null ? "" : id.toString());
        return this;
    }

    @Override
    @Nullable
    public ItemGenericGrenade getGrenade() {
        String raw = this.getEntityData().get(DATA_GRENADE_ITEM);
        if (raw.isEmpty()) return null;
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw));
        return item instanceof ItemGenericGrenade generic ? generic : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GRENADE_ITEM, "");
    }

    @Override
    public void explode() {
        ItemGenericGrenade grenade = this.getGrenade();
        if (!this.level().isClientSide() && grenade != null) {
            grenade.explode(this, this.getThrower(), this.level(), this.getX(), this.getY(), this.getZ());
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("grenade", this.getEntityData().get(DATA_GRENADE_ITEM));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.getEntityData().set(DATA_GRENADE_ITEM, compound.getString("grenade"));
    }
}
