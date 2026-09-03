package com.hbm.blockentity.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code TileEntityFoundryBase} - base class for foundry multiblock components.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryBase.java
 * <p>
 * Stores molten metal (type + amount) and provides standard pour/flow logic for {@link ICrucibleAcceptor}.
 * Child classes: {@link FoundryTankBlockEntity}, foundry channel/basin/cast (if ported).
 */
public abstract class FoundryBaseBlockEntity extends BlockEntity implements ICrucibleAcceptor {

    public NTMMaterial type;
    protected NTMMaterial lastType;
    public int amount;
    protected int lastAmount;

    public FoundryBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (this.lastType != this.type || this.lastAmount != this.amount) {
            if (level != null && (!level.isClientSide || shouldClientReRender())) {
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 2);
                this.lastType = this.type;
                this.lastAmount = this.amount;
                this.setChanged();
            }
        }
    }

    protected boolean shouldClientReRender() {
        return true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("type", this.type == null ? -1 : this.type.id);
        tag.putInt("amount", this.amount);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("type")) {
            int typeId = tag.getInt("type");
            this.type = typeId == -1 ? null : Mats.matById.get(typeId);
        }
        if (tag.contains("amount")) {
            this.amount = tag.getInt("amount");
        }
    }

    public abstract int getCapacity();

    public boolean standardCheck(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        if (this.type != null && this.type != stack.material && this.amount > 0) return false;
        return this.amount < this.getCapacity();
    }

    public Mats.MaterialStack standardAdd(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        this.type = stack.material;

        if (stack.amount + this.amount <= this.getCapacity()) {
            this.amount += stack.amount;
            return null;
        }

        int required = this.getCapacity() - this.amount;
        this.amount = this.getCapacity();

        stack.amount -= required;

        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        return this.standardCheck(world, p, side, stack);
    }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        return this.standardAdd(world, p, side, stack);
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos p, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        if (side != Direction.UP) return false;
        return this.standardCheck(world, p, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos p, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        return this.standardAdd(world, p, side, stack);
    }
}
