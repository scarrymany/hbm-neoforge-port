package com.hbm.blockentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.network.BlockCraneRouter;
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
 * NeoForge port of CE's {@code TileEntityCraneRouter} - routes items to different output directions.
 * Simplified without ModulePatternMatcher/filter inventory:
 * - Round-robin routing through all 6 directions (excluding input side)
 * - No GUI, no filters, no whitelist/blacklist modes
 * Deferred: CE's 30-slot filter inventory + ModulePatternMatcher + mode system (6 sides × 5 filters × 3 modes).
 */
public class CraneRouterBlockEntity extends BlockEntity {

    private int lastOutputIndex = 0; // Round-robin counter

    public CraneRouterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        // Simplified: no active tick logic needed for passive routing
    }

    /**
     * Routes an item to next available output direction (round-robin).
     * CE logic: check filters for all 6 sides, pick matching direction.
     * Simplified: cycle through directions, skip if no conveyor belt present.
     */
    public void routeItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return;
        }

        Direction[] allDirs = Direction.values();
        Direction chosenDir = null;

        // Try up to 6 directions starting from last output
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
            // No conveyor belt found on any side — drop item
            level.addFreshEntity(new ItemEntity(level, 
                    worldPosition.getX() + 0.5, 
                    worldPosition.getY() + 0.5, 
                    worldPosition.getZ() + 0.5, 
                    stack.copy()));
            return;
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
