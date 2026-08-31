package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.DummyBlockEntity;
import com.hbm.blockentity.machine.LaunchInfraBlockEntities;
import com.hbm.blockentity.machine.SiloHatchBlockEntity;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IDoor;
import com.hbm.interfaces.IDummy;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.items.tool.ItemLock;
import com.hbm.items.tool.ItemTooling;
import com.hbm.api.block.IToolable.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * Ported from CE's {@code com.hbm.blocks.machine.DummyBlockSiloHatch} (209 lines, read in full) -
 * the invisible marker block {@link SiloHatchBlockEntity#placeDummy}/{@code removeDummy} places
 * around itself, delegating every interaction back to the {@link SiloHatchBlockEntity} named by its
 * {@link DummyBlockEntity#target} back-reference.
 * <p>
 * <b>Not ported</b>: Galacticraft {@code IPartialSealableBlock} - see {@link BlockSiloHatch}'s
 * identical javadoc note.
 */
public class DummyBlockSiloHatch extends BaseEntityBlock implements IDummy, IBomb, IRadResistantBlock {

    public static final MapCodec<DummyBlockSiloHatch> CODEC = simpleCodec(DummyBlockSiloHatch::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public DummyBlockSiloHatch(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DummyBlockEntity(LaunchInfraBlockEntities.DUMMY_SILO_HATCH.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == LaunchInfraBlockEntities.DUMMY_SILO_HATCH.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    private SiloHatchBlockEntity resolveHatch(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DummyBlockEntity dummy) || dummy.target == null) return null;
        return level.getBlockEntity(dummy.target) instanceof SiloHatchBlockEntity hatch ? hatch : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemKey) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        SiloHatchBlockEntity entity = resolveHatch(level, pos);
        if (entity == null) return InteractionResult.PASS;

        if (held.getItem() instanceof ItemTooling tool && tool.getType() == ToolType.SCREWDRIVER) {
            if (entity.getConfiguredMode() == IDoor.Mode.TOOLABLE) {
                entity.toggleRedstoneMode();
                return InteractionResult.SUCCESS;
            }
        }

        if (entity.isRedstoneOnly()) return InteractionResult.PASS;

        int heldPins = held.getItem() instanceof ItemKeyPin ? ItemKeyPin.getPins(held) : 0;
        boolean universalKey = held.getItem() instanceof ItemKey;
        if (entity.canAccess(heldPins, universalKey)) {
            entity.tryToggle();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            SiloHatchBlockEntity entity = resolveHatch(level, pos);
            if (entity != null && !entity.isLocked()) {
                entity.tryToggle();
                return BombReturnCode.TRIGGERED;
            }
            return BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                        net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(LaunchInfraBlocks.SILO_HATCH.get());
    }

    @Override
    public boolean isRadResistant(Level level, BlockPos pos) {
        if (level != null) {
            SiloHatchBlockEntity entity = resolveHatch(level, pos);
            if (entity != null) return entity.getState() == IDoor.DoorState.CLOSED;
        }
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
