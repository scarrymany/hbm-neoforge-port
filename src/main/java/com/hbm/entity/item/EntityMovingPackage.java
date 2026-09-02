package com.hbm.entity.item;

import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge port of CE's {@code EntityMovingPackage} - package of items moving on conveyor belt.
 * Ported from CE's {@code com.hbm.entity.item.EntityMovingPackage} (read in full).
 * Spawned by {@code crane_boxer}, unpacked by {@code crane_unboxer}.
 */
public class EntityMovingPackage extends EntityMovingConveyorObject implements IConveyorPackage {

    protected ItemStack[] contents = new ItemStack[0];

    public EntityMovingPackage(EntityType<? extends EntityMovingPackage> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data for package - contents are NBT-only
    }

    /**
     * CE's setItemStacks: stores the package contents.
     */
    public void setItemStacks(ItemStack[] stacks) {
        if (stacks == null) {
            this.contents = new ItemStack[0];
            return;
        }
        // Careful copy to avoid external mutation
        this.contents = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            this.contents[i] = stacks[i] == null ? ItemStack.EMPTY : stacks[i].copy();
        }
    }

    @Override
    public ItemStack[] getItemStacks() {
        return contents;
    }

    /**
     * CE's attackEntityFrom: drop all items when damaged.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            for (ItemStack stack : contents) {
                if (!stack.isEmpty()) {
                    ItemEntity drop = new ItemEntity(this.level(), this.getX(), this.getY() + 0.125, this.getZ(), stack.copy());
                    this.level().addFreshEntity(drop);
                }
            }
            this.discard();
        }
        return true;
    }

    /**
     * CE's enterBlock: hand off to IEnterableBlock (crane_unboxer).
     */
    @Override
    public void enterBlock(IEnterableBlock enterable, BlockPos pos, Direction dir) {
        if (enterable.canPackageEnter(this.level(), pos.getX(), pos.getY(), pos.getZ(), dir, this)) {
            enterable.onPackageEnter(this.level(), pos.getX(), pos.getY(), pos.getZ(), dir, this);
            this.discard();
        }
    }

    /**
     * CE's onLeaveConveyor: drop items with momentum when leaving belt.
     */
    @Override
    public boolean onLeaveConveyor() {
        this.discard();

        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                ItemEntity item = new ItemEntity(this.level(), 
                        this.getX() + this.getDeltaMovement().x * 2, 
                        this.getY() + this.getDeltaMovement().y * 2, 
                        this.getZ() + this.getDeltaMovement().z * 2, 
                        stack.copy());
                item.setDeltaMovement(this.getDeltaMovement().x * 2, 0.1, this.getDeltaMovement().z * 2);
                item.hasImpulse = true;
                this.level().addFreshEntity(item);
            }
        }

        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        int count = tag.getInt("count");
        this.contents = new ItemStack[count];
        
        if (tag.contains("contents", Tag.TAG_LIST)) {
            ListTag list = tag.getList("contents", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("slot") & 255;
                if (slot >= 0 && slot < this.contents.length) {
                    ItemStack stack = ItemStack.parseOptional(this.registryAccess(), itemTag);
                    this.contents[slot] = stack;
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();

        for (int i = 0; i < this.contents.length; i++) {
            if (this.contents[i] != null && !this.contents[i].isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("slot", (byte) i);
                this.contents[i].save(this.registryAccess(), itemTag);
                list.add(itemTag);
            }
        }

        tag.put("contents", list);
        tag.putInt("count", this.contents.length);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
