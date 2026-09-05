package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.BlastDoorBlockEntity;
import com.hbm.blockentity.machine.DoorGenericBlockEntities;
import com.hbm.blockentity.machine.DummyBlockEntity;
import com.hbm.blocks.generic.GenericBlocks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IDoor;
import com.hbm.interfaces.IDummy;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemLock;
import com.hbm.items.tool.ItemTooling;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * CE {@code DummyBlockBlast} — collision on, invisible, break without {@link #safeBreak} eats core.
 * TODO(CE: DummyBlockBlast.java:15): Galacticraft {@code IPartialSealableBlock}.
 */
public class DummyBlockBlast extends BaseEntityBlock implements IDummy, IBomb, IRadResistantBlock {

    public static final MapCodec<DummyBlockBlast> CODEC = simpleCodec(DummyBlockBlast::new);
    public static boolean safeBreak = false;

    public DummyBlockBlast(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DummyBlockEntity(DoorGenericBlockEntities.DUMMY_BLAST.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DoorGenericBlockEntities.DUMMY_BLAST.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    private BlastDoorBlockEntity resolve(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DummyBlockEntity dummy) || dummy.target == null) return null;
        return level.getBlockEntity(dummy.target) instanceof BlastDoorBlockEntity door ? door : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !safeBreak && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof DummyBlockEntity dummy && dummy.target != null) {
                level.destroyBlock(dummy.target, true);
            }
        }
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemKey) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        BlastDoorBlockEntity entity = resolve(level, pos);
        if (entity == null) return InteractionResult.FAIL;

        if (held.getItem() instanceof ItemTooling tool && tool.getType() == ToolType.SCREWDRIVER) {
            if (entity.getConfiguredMode() == IDoor.Mode.TOOLABLE) {
                if (!entity.canToggleRedstone(player)) return InteractionResult.FAIL;
                entity.toggleRedstoneMode();
                return InteractionResult.SUCCESS;
            }
        }
        if (entity.isRedstoneOnly()) return InteractionResult.FAIL;
        if (entity.canAccess(player)) {
            entity.tryToggle();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            BlastDoorBlockEntity entity = resolve(level, pos);
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
        return new ItemStack(GenericBlocks.BLAST_DOOR.get());
    }

    @Override
    public boolean isRadResistant(Level level, BlockPos pos) {
        if (level != null) {
            BlastDoorBlockEntity entity = resolve(level, pos);
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
}
