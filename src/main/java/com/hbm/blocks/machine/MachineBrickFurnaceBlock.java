package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineBrickFurnaceBlockEntity;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** CE {@code MachineBrickFurnace} off/on pair — same TE, vanilla smelt + fuel. */
public class MachineBrickFurnaceBlock extends BaseEntityBlock {

    public static final MapCodec<MachineBrickFurnaceBlock> CODEC = simpleCodec(MachineBrickFurnaceBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public MachineBrickFurnaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBrickFurnaceBlockEntity(DummyableProcessBlockEntities.MACHINE_BRICK_FURNACE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_BRICK_FURNACE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineBrickFurnaceBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Exact CE {@code MachineBrickFurnace.updateBlockState} :95-107.
     * Same BE type is valid for both variants — keep TE, copy FACING.
     */
    public static void updateBlockState(boolean isProcessing, Level level, BlockPos pos) {
        BlockState cur = level.getBlockState(pos);
        if (!(cur.getBlock() instanceof MachineBrickFurnaceBlock)) return;
        Block on = DummyableProcessBlocks.MACHINE_FURNACE_BRICK_ON.get();
        Block off = DummyableProcessBlocks.MACHINE_FURNACE_BRICK_OFF.get();
        Block target = isProcessing ? on : off;
        if (cur.is(target)) return;
        if (!cur.is(on) && !cur.is(off)) return;
        Direction facing = cur.getValue(FACING);
        level.setBlock(pos, target.defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
    }

    private static boolean isVariantSwap(BlockState oldState, BlockState newState) {
        Block a = oldState.getBlock();
        Block b = newState.getBlock();
        if (!(a instanceof MachineBrickFurnaceBlock) || !(b instanceof MachineBrickFurnaceBlock)) return false;
        return a != b;
    }

    /** CE {@code keepInventory} — do not evict the TE when swapping off↔on. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (isVariantSwap(state, newState)) return;
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** CE {@code getItemDropped}/{@code getItem} — always the off block. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(DummyableProcessBlocks.MACHINE_FURNACE_BRICK_OFF.get());
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(DummyableProcessBlocks.MACHINE_FURNACE_BRICK_OFF.get()));
    }
}
