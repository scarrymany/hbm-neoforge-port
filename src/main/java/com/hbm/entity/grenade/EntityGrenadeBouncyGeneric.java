package com.hbm.entity.grenade;

import com.hbm.items.weapon.ItemGenericGrenade;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.entity.grenade.EntityGrenadeBouncyGeneric} (71 lines) - the fuse-timed
 * bouncing throwable used by {@code stick_dynamite}/{@code stick_dynamite_fishing}. See
 * {@link EntityGrenadeBase}'s javadoc for why this extends that class (bounce-on-impact override)
 * rather than porting CE's separate {@code EntityGrenadeBouncyBase} custom-movement hierarchy.
 * <p>
 * <b>Item-reference sync - a confirmed necessary deviation from CE.</b> CE syncs
 * {@code Item.getIdFromItem(grenade)} (a raw, construction-order-dependent int) via a
 * {@code DataParameter<Integer>}; 1.21.1's registry model has no such numeric id, so this syncs the
 * item's {@link ResourceLocation} as a string instead (the same technique
 * {@code com.hbm.entity.projectile.EntityBulletBaseMK4} already uses for its own
 * {@code BulletConfig} reference), resolved back via {@link BuiltInRegistries#ITEM}.
 */
public class EntityGrenadeBouncyGeneric extends EntityGrenadeBase implements IGenericGrenade {

    private static final EntityDataAccessor<String> DATA_GRENADE_ITEM =
            SynchedEntityData.defineId(EntityGrenadeBouncyGeneric.class, EntityDataSerializers.STRING);

    public EntityGrenadeBouncyGeneric(EntityType<? extends EntityGrenadeBouncyGeneric> type, Level level) {
        super(type, level);
    }

    public EntityGrenadeBouncyGeneric(Level level, LivingEntity thrower, InteractionHand hand) {
        super(GrenadeEntityTypes.GRENADE_BOUNCY_GENERIC.get(), level, thrower, hand);
    }

    public EntityGrenadeBouncyGeneric setType(ItemGenericGrenade grenade) {
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
    public void tick() {
        super.tick();
        ItemGenericGrenade grenade = this.getGrenade();
        if (!this.level().isClientSide() && grenade != null && this.getTimer() >= grenade.getMaxTimer()) {
            this.explode();
        }
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!(mop instanceof BlockHitResult bhr)) {
            // an entity hit doesn't bounce in CE either (moveBounce only reacts to block collision) -
            // fall through to the base class's immediate-detonate behavior for a living-entity hit.
            super.onImpact(mop);
            return;
        }

        Direction dir = bhr.getDirection();
        Vec3 hit = bhr.getLocation();
        this.setPos(hit.x + dir.getStepX() * 0.05, hit.y + dir.getStepY() * 0.05, hit.z + dir.getStepZ() * 0.05);

        ItemGenericGrenade grenade = this.getGrenade();
        double bounceMod = grenade != null ? grenade.getBounceMod() : 0.5D;
        Vec3 motion = this.getDeltaMovement();
        double mx = dir.getStepX() != 0 ? motion.x * -bounceMod : motion.x * 0.8D;
        double my = dir.getStepY() != 0 ? motion.y * -bounceMod : motion.y * 0.8D;
        double mz = dir.getStepZ() != 0 ? motion.z * -bounceMod : motion.z * 0.8D;
        this.setDeltaMovement(mx, my, mz);
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
