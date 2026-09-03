package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.DroneCrateProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code drone_crate_provider} - DroneDock-style block that pushes items into delivery drones.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/ModBlocks.java:1127 (uses DroneDock class)
 * <p>
 * Minimal implementation: Container inventory. When drone arrives nearby, push matching items from provider into drone cargo.
 * TODO(CE): Full RequestNetwork integration (OfferNode pathfinding, network-wide offers).
 */
public class BlockDroneCrateProvider extends Block implements EntityBlock {

    public BlockDroneCrateProvider(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneCrateProviderBlockEntity(DroneBlocks.DRONE_CRATE_PROVIDER_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DroneBlocks.DRONE_CRATE_PROVIDER_BE_TYPE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DroneCrateProviderBlockEntity provider) {
            player.openMenu(provider);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
