package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineFanBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code MachineFan} — 1×1 directional blower. No CE container.
 * Tool toggle Exact CE {@code MachineFan.java:267-327}: screwdriver flips face;
 * hand-drill {@code falloff}; defuser {@code suck}; {@code LEVER_CLICK} 0.5F/0.5F.
 */
public class MachineFanBlock extends BaseEntityBlock implements IToolable {

    public static final MapCodec<MachineFanBlock> CODEC = simpleCodec(MachineFanBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MachineFanBlock(Properties properties) {
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
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
        return new MachineFanBlockEntity(DummyableProcessBlockEntities.FAN.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.FAN.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineFanBlockEntity fan) {
            fan.setIndirectlyPowered(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        BlockPos pos = new BlockPos(x, y, z);

        if (tool == ToolType.SCREWDRIVER) {
            BlockState state = world.getBlockState(pos);
            world.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getOpposite()), 3);
            return true;
        }

        if (tool == ToolType.HAND_DRILL) {
            if (world.getBlockEntity(pos) instanceof MachineFanBlockEntity tile) {
                tile.falloff = !tile.falloff;
                tile.setChanged();
                if (!world.isClientSide) {
                    // Exact CE MachineFan.java:285-295
                    player.displayClientMessage(
                            Component.translatable("tile.fan" + (tile.falloff ? ".falloffOn" : ".falloffOff"))
                                    .withStyle(ChatFormatting.GOLD),
                            true);
                    world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 0.5F);
                }
            }
            return true;
        }

        if (tool == ToolType.DEFUSER) {
            if (world.getBlockEntity(pos) instanceof MachineFanBlockEntity tile) {
                tile.suck = !tile.suck;
                tile.setChanged();
                if (!world.isClientSide) {
                    // Exact CE MachineFan.java:310-320
                    player.displayClientMessage(
                            Component.translatable("tile.fan" + (tile.suck ? ".suckOn" : ".suckOff"))
                                    .withStyle(ChatFormatting.GOLD),
                            true);
                    world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 0.5F);
                }
            }
            return true;
        }

        return false;
    }
}
