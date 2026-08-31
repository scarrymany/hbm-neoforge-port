package com.hbm.entity.cart;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartNTM} (93 lines, read in full) - the shared
 * base of the whole reskinned-vanilla-minecart family, per
 * {@code docs/phase4/entities_vehicles_aircraft.md}'s minecart section (Headline finding #3: "zero
 * custom movement physics anywhere in this family" - confirmed by reading every subclass; rail
 * following is 100% inherited from vanilla {@link AbstractMinecart}, never overridden here).
 * <p>
 * <b>{@code CART_BASE} - purely cosmetic, ported as a plain synced int, no skin-variant item work.</b>
 * CE's {@code EnumCartBase} (VANILLA/WOOD/STEEL/PAINTED) selects which vanilla-cart texture renders
 * under this mod's overlay - "not a gameplay stat" per the report's own framing. The synced field
 * itself is ported faithfully (any future renderer or command can still read/set it); the *item* side
 * (CE's {@code ItemModMinecart}, a full 1.12 dynamic-model-baking class with 4 skins x 5 cart types)
 * is real, un-owned {@code items/tool} scope this package does not claim - see this package's own
 * {@code knownGaps}.
 * <p>
 * <b>{@code killMinecart}/{@code getCartItem} - reimplemented directly on {@link
 * net.minecraft.world.entity.Entity#hurt}, not on {@code AbstractMinecart}'s own built-in
 * cumulative-damage/{@code destroy} pipeline.</b> This port's own {@code hurt} override triggers
 * {@link #killMinecart} on any non-invulnerable hit (a documented simplification of vanilla/CE's
 * "several hits or a big single hit" damage-accumulation model - see this package's own
 * {@code knownGaps} for why: this port has no already-compiling reference to
 * {@code AbstractMinecart}'s own internal damage-threshold/{@code destroy} method names to build on
 * safely in this sandbox). What <i>is</i> preserved exactly is CE's actual mod-specific behavior: on
 * death, drop {@link #getCartItem()} (each subclass's own item form), preserving a custom name if set,
 * via vanilla {@code Entity#spawnAtLocation} - confirmed real and already used elsewhere in this port.
 */
public abstract class EntityMinecartNTM extends AbstractMinecart {

    private static final EntityDataAccessor<Integer> CART_BASE =
            SynchedEntityData.defineId(EntityMinecartNTM.class, EntityDataSerializers.INT);

    protected EntityMinecartNTM(EntityType<? extends EntityMinecartNTM> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CART_BASE, 0);
    }

    /** CE: {@code setBase(EnumCartBase)} - see class javadoc, a plain cosmetic int here (0-3). */
    public void setBase(int base) {
        this.entityData.set(CART_BASE, base);
    }

    /** CE: {@code getBase()}. */
    public int getBase() {
        return this.entityData.get(CART_BASE);
    }

    /** CE: {@code canBeCollidedWith() { return true; }} */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public Type getMinecartType() {
        return Type.RIDEABLE;
    }

    @Override
    protected Item getDropItem() {
        return getCartItem();
    }

    /** CE: {@code getCartItem()} - each subclass's own item form. */
    public abstract Item getCartItem();

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (!this.level().isClientSide() && this.isAlive()) {
            this.killMinecart(source);
        }
        return true;
    }

    /**
     * CE: {@code killMinecart(DamageSource)} - dies and drops {@link #getCartItem()}, preserving a
     * custom name tag if one was set. {@link EntityMinecartCrate} overrides this to add its NBT-size
     * safety-valve explosion (see that class's own javadoc) before calling back into this method.
     */
    protected void killMinecart(DamageSource source) {
        this.discard();
        ItemStack stack = new ItemStack(getCartItem());
        if (this.hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        this.spawnAtLocation(stack, 0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("base", this.getBase());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setBase(tag.getInt("base"));
    }
}
