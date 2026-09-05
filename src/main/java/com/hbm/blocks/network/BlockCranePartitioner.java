package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blockentity.network.CranePartitionerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE's {@code CranePartitioner} - splits packages into individual items.
 * Simplified without custom model/rendering: standard block model.
 * CE behavior: receives EntityMovingPackage, distributes items round-robin to adjacent conveyors.
 * Deferred: CE's custom OBJ model + CranePartitionerBakedModel rendering.
 */
public class BlockCranePartitioner extends HorizontalDirectionalBlock implements EntityBlock, IEnterableBlock {

    public static final MapCodec<BlockCranePartitioner> CODEC = simpleCodec(BlockCranePartitioner::new);

    public BlockCranePartitioner(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CranePartitionerBlockEntity(CraneBlocks.CRANE_PARTITIONER_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof CranePartitionerBlockEntity partitioner) {
                partitioner.tick();
            }
        };
    }

    @Override
    public boolean canItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity) {
        return false; // Partitioner only accepts packages, not individual items
    }

    @Override
    public void onItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity) {
        // No-op: partitioner doesn't accept individual items
    }

    @Override
    public boolean canPackageEnter(Level world, int x, int y, int z, Direction dir, IConveyorPackage entity) {
        return true;
    }

    @Override
    public void onPackageEnter(Level world, int x, int y, int z, Direction dir, IConveyorPackage entity) {
        if (entity == null || entity.getItemStacks() == null || entity.getItemStacks().length == 0) {
            return;
        }

        BlockEntity be = world.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof CranePartitionerBlockEntity partitioner) {
            partitioner.partitionPackage(entity.getItemStacks());
        }
    }
}
