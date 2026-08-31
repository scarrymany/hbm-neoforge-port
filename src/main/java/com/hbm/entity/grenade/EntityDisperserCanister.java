package com.hbm.entity.grenade;

import com.hbm.entity.effect.EntityMist;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
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
 * Port of CE's {@code com.hbm.entity.grenade.EntityDisperserCanister} (93 lines) - the
 * {@code ItemDisperser} family's throw entity. Reuses {@link EntityGrenadeBase}'s shared
 * throw-launch physics and immediate-on-impact detonation exactly as CE does.
 * <p>
 * <b>{@code EntityMist} payload - wired for real</b> (Phase 4,
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md}): {@link #explode()} spawns CE's real
 * payload, a {@link EntityMist} sized {@code 10x5}, {@code 80}-tick duration, typed by
 * {@link #getFluid()} - matching CE's own {@code explode()} exactly (read in full).
 */
public class EntityDisperserCanister extends EntityGrenadeBase {

    private static final EntityDataAccessor<Integer> DATA_FLUID_ID =
            SynchedEntityData.defineId(EntityDisperserCanister.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ITEM =
            SynchedEntityData.defineId(EntityDisperserCanister.class, EntityDataSerializers.STRING);

    public EntityDisperserCanister(EntityType<? extends EntityDisperserCanister> type, Level level) {
        super(type, level);
    }

    public EntityDisperserCanister(Level level, LivingEntity thrower, InteractionHand hand) {
        super(GrenadeEntityTypes.DISPERSER_CANISTER.get(), level, thrower, hand);
    }

    public FluidType getFluid() {
        return Fluids.fromID(this.getEntityData().get(DATA_FLUID_ID));
    }

    public EntityDisperserCanister setFluid(FluidType type) {
        this.getEntityData().set(DATA_FLUID_ID, type.getID());
        return this;
    }

    @Nullable
    public Item getCanisterItem() {
        String raw = this.getEntityData().get(DATA_ITEM);
        return raw.isEmpty() ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw));
    }

    public EntityDisperserCanister setType(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        this.getEntityData().set(DATA_ITEM, id == null ? "" : id.toString());
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLUID_ID, 0);
        builder.define(DATA_ITEM, "");
    }

    @Override
    public void explode() {
        if (!this.level().isClientSide()) {
            EntityMist.spawn(this.level(), this.getX(), this.getY(), this.getZ(), getFluid(), 10F, 5F, 80);
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("fluid", this.getEntityData().get(DATA_FLUID_ID));
        compound.putString("item", this.getEntityData().get(DATA_ITEM));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.getEntityData().set(DATA_FLUID_ID, compound.getInt("fluid"));
        this.getEntityData().set(DATA_ITEM, compound.getString("item"));
    }
}
