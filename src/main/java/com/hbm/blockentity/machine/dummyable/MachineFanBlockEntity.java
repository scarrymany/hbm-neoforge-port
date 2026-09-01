package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blocks.machine.MachineFanBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * CE {@code MachineFan.TileEntityFan} — redstone ray 10, push 0.1.
 * {@code IBlowable} skipped (API not ported). No CE container.
 */
public class MachineFanBlockEntity extends BlockEntity implements ITickableBE {

    public boolean falloff = true;
    public boolean suck = false;
    public boolean isIndirectlyPowered;

    public MachineFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide && level.getGameTime() % 20 == 0) {
            boolean powered = level.hasNeighborSignal(worldPosition);
            if (isIndirectlyPowered != powered) {
                isIndirectlyPowered = powered;
                setChanged();
            }
        }

        if (!isIndirectlyPowered) return;

        Direction dir = getBlockState().getValue(MachineFanBlock.FACING);
        int range = 10;
        int effRange = 0;
        for (int i = 1; i <= range; i++) {
            BlockPos p = worldPosition.relative(dir, i);
            BlockState s = level.getBlockState(p);
            if (s.isCollisionShapeFullBlock(level, p)) break;
            effRange = i;
        }

        int x = dir.getStepX() * effRange;
        int y = dir.getStepY() * effRange;
        int z = dir.getStepZ() * effRange;
        AABB aabb = new AABB(
                worldPosition.getX() + 0.5 + Math.min(x, 0),
                worldPosition.getY() + 0.5 + Math.min(y, 0),
                worldPosition.getZ() + 0.5 + Math.min(z, 0),
                worldPosition.getX() + 0.5 + Math.max(x, 0),
                worldPosition.getY() + 0.5 + Math.max(y, 0),
                worldPosition.getZ() + 0.5 + Math.max(z, 0)
        ).inflate(0.5, 0.5, 0.5);

        for (Entity e : level.getEntities(null, aabb)) {
            double coeff = 0.1D;
            if (falloff) {
                double dist = e.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
                coeff *= 1.5D * (1.0D - Math.sqrt(dist) / range / 2.0D);
            }
            if (suck) coeff *= -1.0D;
            e.setDeltaMovement(e.getDeltaMovement().add(dir.getStepX() * coeff, dir.getStepY() * coeff, dir.getStepZ() * coeff));
        }
    }

    public void setIndirectlyPowered(boolean powered) {
        this.isIndirectlyPowered = powered;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("falloff", falloff);
        tag.putBoolean("suck", suck);
        tag.putBoolean("powered", isIndirectlyPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        falloff = tag.getBoolean("falloff");
        suck = tag.getBoolean("suck");
        isIndirectlyPowered = tag.getBoolean("powered");
    }
}
