package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.BlastDoorBlockEntity;
import com.hbm.blockentity.machine.DoorGenericBlockEntities;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IDoor;
import com.hbm.interfaces.IMultiBlock;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemLock;
import com.hbm.items.tool.ItemTooling;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * CE {@code BlastDoor} — NOT {@link com.hbm.blocks.generic.BlockDoorGeneric}.
 * TODO(CE: BlastDoor.java:14): Galacticraft {@code IPartialSealableBlock}.
 */
public class BlastDoor extends BaseEntityBlock implements IBomb, IMultiBlock, IRadResistantBlock {

    public static final MapCodec<BlastDoor> CODEC = simpleCodec(BlastDoor::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlastDoor(Properties properties) {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlastDoorBlockEntity(DoorGenericBlockEntities.BLAST_DOOR.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DoorGenericBlockEntities.BLAST_DOOR.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlastDoorBlockEntity entity) {
            if (!entity.isLocked()) {
                entity.tryToggle();
                return BombReturnCode.TRIGGERED;
            }
            return BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemKey) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlastDoorBlockEntity entity)) return InteractionResult.FAIL;

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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BlastDoorBlockEntity te)) return;
        if (!(te.placeDummy(pos.above(1))
                && te.placeDummy(pos.above(2))
                && te.placeDummy(pos.above(3))
                && te.placeDummy(pos.above(4))
                && te.placeDummy(pos.above(5))
                && te.placeDummy(pos.above(6)))) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean isRadResistant(Level level, BlockPos pos) {
        if (level != null && level.getBlockEntity(pos) instanceof IDoor door) {
            return door.getState() == IDoor.DoorState.CLOSED;
        }
        return false;
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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§2[").append(Component.translatable("trait.radshield")).append("]"));
        float hardness = this.getExplosionResistance();
        if (hardness > 50) {
            tooltip.add(Component.translatable("trait.blastres", hardness).withStyle(ChatFormatting.GOLD));
        }
    }
}
