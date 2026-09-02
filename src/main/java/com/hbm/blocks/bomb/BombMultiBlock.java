package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.BombBlockEntities;
import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.interfaces.IBomb;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code BombMulti} (262 lines). Custom-bomb assembly block with 6-slot GUI
 * (4 TNT corners + 2 modifiers). Formula: base 8.0 + modifier math for explosion/cluster/fire/poison/gas.
 */
public class BombMultiBlock extends BaseEntityBlock implements IBomb {

    public static final MapCodec<BombMultiBlock> CODEC = simpleCodec(BombMultiBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

    public final float explosionBaseValue = 8.0F;
    public float explosionValue = 0.0F;
    public int clusterCount = 0;
    public int fireRadius = 0;
    public int poisonRadius = 0;
    public int gasCloud = 0;

    public BombMultiBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BombMultiBlockEntity(BombBlockEntities.BOMB_MULTI.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && !player.isCrouching()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BombMultiBlockEntity bombMulti) {
                player.openMenu(bombMulti);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BombMultiBlockEntity bombMulti && bombMulti.isLoaded()) {
                level.removeBlock(pos, false);
                igniteTestBomb(level, null, pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BombMultiBlockEntity bombMulti) {
                for (int i = 0; i < bombMulti.inventory.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bombMulti.inventory.getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    public void igniteTestBomb(Level level, @Nullable Entity detonator, BlockPos pos) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BombMultiBlockEntity bombMulti) || !bombMulti.isLoaded()) return;

        this.explosionValue = this.explosionBaseValue;
        applyModifier(bombMulti.return2type());
        applyModifier(bombMulti.return5type());

        bombMulti.clearSlots();
        level.removeBlock(pos, false);

        ExplosionLarge.explode(level, detonator, pos.getX(), pos.getY(), pos.getZ(), explosionValue, true, true, true);
        this.explosionValue = 0;

        if (this.clusterCount > 0) {
            ExplosionChaos.cluster(level, pos.getX(), pos.getY(), pos.getZ(), this.clusterCount, 0.5);
        }
        if (this.fireRadius > 0) {
            ExplosionChaos.burn(level, null, pos, this.fireRadius);
        }
        if (this.poisonRadius > 0) {
            ExplosionNukeGeneric.waste(level, pos.getX(), pos.getY(), pos.getZ(), this.poisonRadius);
        }
        if (this.gasCloud > 0) {
            ExplosionChaos.spawnChlorine(level, pos.getX(), pos.getY(), pos.getZ(), this.gasCloud, this.gasCloud / 50, 0);
        }

        this.clusterCount = 0;
        this.fireRadius = 0;
        this.poisonRadius = 0;
        this.gasCloud = 0;
    }

    private void applyModifier(int type) {
        switch (type) {
            case 1 -> this.explosionValue += 1.0F;
            case 2 -> this.explosionValue += 4.0F;
            case 3 -> this.clusterCount += 50;
            case 4 -> this.fireRadius += 10;
            case 5 -> this.poisonRadius += 15;
            case 6 -> this.gasCloud += 50;
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BombMultiBlockEntity bombMulti && bombMulti.isLoaded()) {
                level.removeBlock(pos, false);
                igniteTestBomb(level, detonator, pos);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return BombReturnCode.UNDEFINED;
    }
}
