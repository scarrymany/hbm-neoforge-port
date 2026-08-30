package com.hbm.entity.item;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IEnterableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.item.EntityMovingItem} (read in full) - the one conveyor
 * blocks actually spawn. CE's {@code DataParameter<ItemStack>}/{@code EntityDataManager} pair maps
 * to {@link SynchedEntityData}/{@link EntityDataAccessor} here, with
 * {@code DataSerializers.ITEM_STACK} -> {@link EntityDataSerializers#ITEM_STACK} - confirmed shape,
 * cross-checked against Neo Edition's real (confirmed-working) {@code com.hbm.entity.item.MovingItem}
 * sibling class.
 * <p>
 * <b>Behavior note</b>: CE additionally overrides {@code hitByEntity(Entity)} on the abstract base to
 * instantly {@code setDead()} (no item drop) when hit by a player, with {@code attackEntityFrom}
 * (this class) only reached for non-player damage sources. Modern {@code Entity} has no direct
 * equivalent single-purpose hook for "let the target fully intercept a player's melee attack" that
 * both this port and Neo Edition's own (confirmed-shape) {@code MovingItem} rely on - Neo Edition
 * drops the instant-kill-on-player-punch special case entirely and only overrides
 * {@link #hurt(DamageSource, float)}, so a player punching this entity now goes through the same
 * drop-as-{@link ItemEntity}-and-discard path as any other damage source. This port follows Neo
 * Edition's simplification here rather than reintroducing a bespoke attack-interception hook for one
 * minor edge case.
 */
public class EntityMovingItem extends EntityMovingConveyorObject implements IConveyorItem {

    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(EntityMovingItem.class, EntityDataSerializers.ITEM_STACK);

    /** CE round-trips this through NBT but never reads it back anywhere in the source tree; kept for
     * 1:1 NBT-shape parity with CE saves rather than dropped as dead weight. */
    private int schedule = 0;

    public EntityMovingItem(EntityType<? extends EntityMovingItem> entityType, Level level) {
        super(entityType, level);
    }

    public void setItemStack(ItemStack stack) {
        this.entityData.set(STACK, stack);
    }

    @Override
    public ItemStack getItemStack() {
        return this.entityData.get(STACK);
    }

    /** Ensures the item is knocked off the belt (dropped as a normal item) by any damage source. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItemStack()));
            this.discard();
        }
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STACK, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ItemStack stack = tag.contains("Item", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Item"))
                : ItemStack.EMPTY;

        this.setItemStack(stack);
        this.schedule = tag.getInt("schedule");

        if (stack.isEmpty()) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ItemStack stack = this.getItemStack();
        if (!stack.isEmpty()) {
            tag.put("Item", stack.save(this.registryAccess()));
        }
        tag.putInt("schedule", this.schedule);
    }

    @Override
    public void enterBlock(IEnterableBlock enterable, BlockPos pos, Direction dir) {
        Level level = this.level();
        if (enterable.canItemEnter(level, pos.getX(), pos.getY(), pos.getZ(), dir, this)) {
            enterable.onItemEnter(level, pos.getX(), pos.getY(), pos.getZ(), dir, this);
            this.discard();
        }
    }

    @Override
    public boolean onLeaveConveyor() {
        if (this.isRemoved()) return true;

        this.discard();

        Level level = this.level();
        ItemEntity item = new ItemEntity(level,
                this.getX() + this.getDeltaMovement().x * 2,
                this.getY() + this.getDeltaMovement().y * 2,
                this.getZ() + this.getDeltaMovement().z * 2,
                this.getItemStack());
        item.setDeltaMovement(this.getDeltaMovement().x * 2, 0.1D, this.getDeltaMovement().z * 2);
        item.hasImpulse = true;
        level.addFreshEntity(item);

        return true;
    }
}
