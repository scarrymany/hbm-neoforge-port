package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
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
 * NeoForge port of CE's {@code CraneRouter} - routes items to different directions.
 * Simplified without ModulePatternMatcher: round-robin routing through available outputs.
 * Deferred: CE's 30-slot filter inventory + per-side whitelist/blacklist/wildcard modes.
 */
public class BlockCraneRouter extends HorizontalDirectionalBlock implements EntityBlock, IEnterableBlock {

    public static final MapCodec<BlockCraneRouter> CODEC = simpleCodec(BlockCraneRouter::new);

    public BlockCraneRouter(BlockBehaviour.Properties props) {
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
        return new CraneRouterBlockEntity(CraneBlocks.CRANE_ROUTER_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof CraneRouterBlockEntity router) {
                router.tick();
            }
        };
    }

    @Override
    public boolean canItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity) {
        return true;
    }

    @Override
    public void onItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity) {
        if (entity == null || entity.getItemStack().isEmpty()) {
            return;
        }

        BlockEntity be = world.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof CraneRouterBlockEntity router) {
            router.routeItem(entity.getItemStack().copy());
        } else {
            // Fallback: drop item
            world.addFreshEntity(new ItemEntity(world, x + 0.5, y + 0.5, z + 0.5, entity.getItemStack().copy()));
        }
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
        if (be instanceof CraneRouterBlockEntity router) {
            for (net.minecraft.world.item.ItemStack stack : entity.getItemStacks()) {
                if (!stack.isEmpty()) {
                    router.routeItem(stack.copy());
                }
            }
        } else {
            // Fallback: drop items
            for (net.minecraft.world.item.ItemStack stack : entity.getItemStacks()) {
                if (!stack.isEmpty()) {
                    world.addFreshEntity(new ItemEntity(world, x + 0.5, y + 0.5, z + 0.5, stack.copy()));
                }
            }
        }
    }
}
