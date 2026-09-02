package com.hbm.blockentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * NeoForge port of CE's {@code TileEntityCranePartitioner} - splits packages into individual items.
 * CE behavior: receives EntityMovingPackage, distributes items round-robin to adjacent conveyors.
 * Simplified: no filter inventory, no sorting logic — just round-robin distribution.
 */
public class CranePartitionerBlockEntity extends BlockEntity {

    private int lastOutputIndex = 0; // Round-robin counter for 6 directions

    public CranePartitionerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        // Simplified: no active tick logic needed for passive partitioning
    }

    /**
     * CE logic: receives package, splits items to different output directions based on filters/sorting.
     * Simplified: round-robin distribution to any adjacent conveyor belt.
     */
    public void partitionPackage(ItemStack[] items) {
        if (level == null || items == null || items.length == 0) {
            return;
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            Direction chosenDir = null;

            // Try up to 6 directions starting from last output
            Direction[] allDirs = Direction.values();
            for (int i = 0; i < 6; i++) {
                int tryIndex = (lastOutputIndex + i) % 6;
                Direction tryDir = allDirs[tryIndex];

                BlockPos targetPos = worldPosition.relative(tryDir);
                if (level.getBlockState(targetPos).getBlock() instanceof IConveyorBelt) {
                    chosenDir = tryDir;
                    lastOutputIndex = (tryIndex + 1) % 6; // Advance for next item
                    break;
                }
            }

            if (chosenDir == null) {
                // No conveyor belt found — drop item
                level.addFreshEntity(new ItemEntity(level, 
                        worldPosition.getX() + 0.5, 
                        worldPosition.getY() + 0.5, 
                        worldPosition.getZ() + 0.5, 
                        stack.copy()));
                continue;
            }

            // Spawn EntityMovingItem on chosen conveyor
            BlockPos targetPos = worldPosition.relative(chosenDir);
            IConveyorBelt belt = (IConveyorBelt) level.getBlockState(targetPos).getBlock();

            EntityMovingItem item = new EntityMovingItem(ConveyorEntityTypes.MOVING_ITEM.get(), level);
            Vec3 spawnPos = new Vec3(
                    worldPosition.getX() + 0.5 + chosenDir.getStepX() * 0.55,
                    worldPosition.getY() + 0.5 + chosenDir.getStepY() * 0.55,
                    worldPosition.getZ() + 0.5 + chosenDir.getStepZ() * 0.55
            );
            Vec3 snap = belt.getClosestSnappingPosition(level, targetPos, spawnPos);
            item.setPos(snap.x, snap.y, snap.z);
            item.setItemStack(stack.copy());
            level.addFreshEntity(item);
        }

        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("LastOutputIndex", lastOutputIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastOutputIndex = tag.getInt("LastOutputIndex");
    }
}
