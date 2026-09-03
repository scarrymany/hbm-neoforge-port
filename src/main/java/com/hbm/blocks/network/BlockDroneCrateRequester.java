package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.DroneCrateRequesterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code drone_crate_requester} - DroneDock-style block that pulls items from delivery drones.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/ModBlocks.java:1128 (uses DroneDock class)
 * <p>
 * Minimal implementation: Container + filter slots. When drone arrives with matching items, pull into requester inventory.
 * TODO(CE): Full RequestNetwork integration (RequestNode pathfinding, ModulePatternMatcher filters, network-wide requests).
 */
public class BlockDroneCrateRequester extends Block implements EntityBlock {

    public BlockDroneCrateRequester(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneCrateRequesterBlockEntity(DroneBlocks.DRONE_CRATE_REQUESTER_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DroneBlocks.DRONE_CRATE_REQUESTER_BE_TYPE.get() ? ITickableBE.ticker() : null;
    }
}
