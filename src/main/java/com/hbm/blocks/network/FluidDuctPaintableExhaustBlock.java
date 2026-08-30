package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.network.FluidDuctBlockEntities;
import com.hbm.blockentity.network.PipeExhaustPaintableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Paintable exhaust variant, ported from CE's
 * {@code com.hbm.blocks.network.FluidDuctPaintableBlockExhaust} - same scope reduction as
 * {@link FluidDuctPaintableBlock} (CTM/baked-model rendering deferred, disguise + screwdriver/defuser
 * interaction kept), layered onto {@link PipeExhaustPaintableBlockEntity} instead of
 * {@code PipePaintableBlockEntity} for the three-smoke-node lifecycle.
 */
public class FluidDuctPaintableExhaustBlock extends FluidDuctBaseBlock implements IToolable {

    public static final BooleanProperty DEFUSED = FluidDuctPaintableBlock.DEFUSED;

    public FluidDuctPaintableExhaustBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DEFUSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEFUSED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeExhaustPaintableBlockEntity(FluidDuctBlockEntities.PAINTABLE_EXHAUST_TYPE.get(), pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(stack.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() == this) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof PipeExhaustPaintableBlockEntity pipe) || pipe.getDisguise() != null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        pipe.setDisguise(blockItem.getBlock());
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                            InteractionHand hand, ToolType tool) {
        BlockPos pos = new BlockPos(x, y, z);

        if (tool == ToolType.SCREWDRIVER) {
            if (!(world.getBlockEntity(pos) instanceof PipeExhaustPaintableBlockEntity pipe) || pipe.getDisguise() == null) return false;
            if (!world.isClientSide) pipe.setDisguise(null);
            return true;
        }

        if (tool == ToolType.DEFUSER) {
            if (!world.isClientSide) {
                BlockState state = world.getBlockState(pos);
                world.setBlock(pos, state.setValue(DEFUSED, !state.getValue(DEFUSED)), 3);
            }
            return true;
        }

        return false;
    }
}
