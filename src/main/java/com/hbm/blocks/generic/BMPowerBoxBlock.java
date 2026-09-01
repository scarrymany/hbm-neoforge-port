package com.hbm.blocks.generic;

import com.hbm.blockentity.machine.dummyable.BMPowerBoxBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.lib.HBMSoundHandler;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code BMPowerBox} — 1×1 lever, redstone 15 when on. No GUI.
 * Control-panel IControllable TODO(CE: TileEntityBMPowerBox.java:52-83).
 */
public class BMPowerBoxBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty IS_ON = BooleanProperty.create("is_on");
    public static final MapCodec<BMPowerBoxBlock> CODEC = simpleCodec(BMPowerBoxBlock::new);

    private static final VoxelShape NORTH = Block.box(4.0, 2.7, 0.0, 12.0, 12.2, 1.9);
    private static final VoxelShape SOUTH = Block.box(4.0, 2.7, 14.1, 12.0, 12.2, 16.0);
    private static final VoxelShape WEST = Block.box(0.0, 2.7, 4.0, 1.9, 12.2, 12.0);
    private static final VoxelShape EAST = Block.box(14.1, 2.7, 4.0, 16.0, 12.2, 12.0);

    public BMPowerBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(IS_ON, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_ON);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(IS_ON, false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BMPowerBoxBlockEntity(DummyableProcessBlockEntities.BM_POWER_BOX.get(), pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BMPowerBoxBlockEntity box)) return InteractionResult.PASS;
        if (level.getGameTime() - box.ticksPlaced < 12) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        boolean wasOn = state.getValue(IS_ON);
        level.playSound(null, pos, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 1F, wasOn ? 0.9F : 1F);
        level.setBlock(pos, state.setValue(IS_ON, !wasOn), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BMPowerBoxBlockEntity fresh) {
            fresh.ticksPlaced = level.getGameTime();
        }
        player.swing(player.getUsedItemHand());
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(IS_ON) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(IS_ON) ? 15 : 0;
    }
}
