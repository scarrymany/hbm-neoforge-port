package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.LaunchInfraBlockEntities;
import com.hbm.blockentity.machine.SiloHatchBlockEntity;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IDoor;
import com.hbm.interfaces.IMultiBlock;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.items.tool.ItemLock;
import com.hbm.items.tool.ItemTooling;
import com.hbm.api.block.IToolable.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
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

/**
 * Ported from CE's {@code com.hbm.blocks.machine.BlockSiloHatch} (237 lines, read in full) - a
 * hand-rolled dummy-placement multiblock (NOT {@code BlockDummyable}), the "real" blast door only
 * {@code silo_hatch_drillgon} uses (per {@code docs/phase3/missile_launch_infra.md}'s explicit
 * naming-trap warning - {@code silo_hatch}/{@code silo_hatch_large} are unrelated generic doors,
 * Phase 1 scope, not this class).
 * <p>
 * <b>Not ported</b>: Galacticraft {@code IPartialSealableBlock} integration - a third-party mod
 * bridge with no mod-wide Galacticraft-parity decision anywhere in this port (same open policy
 * question as OpenComputers, see {@link com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity}'s
 * javadoc for the identical precedent), dropped entirely rather than guessed at here.
 */
public class BlockSiloHatch extends BaseEntityBlock implements IBomb, IMultiBlock, IRadResistantBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockSiloHatch(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return new SiloHatchBlockEntity(LaunchInfraBlockEntities.SILO_HATCH.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == LaunchInfraBlockEntities.SILO_HATCH.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof SiloHatchBlockEntity entity) {
                if (!entity.isLocked()) {
                    entity.tryToggle();
                    return BombReturnCode.TRIGGERED;
                }
                return BombReturnCode.ERROR_INCOMPATIBLE;
            }
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemKey) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof SiloHatchBlockEntity entity)) return InteractionResult.PASS;

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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SiloHatchBlockEntity hatch)) return;

        Direction facing = state.getValue(FACING).getOpposite();
        BlockPos center = pos.relative(facing, 3);
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                // cut out the corners
                if ((Math.abs(i) == 3 && Math.abs(j) == 3) || (Math.abs(i) == 2 && Math.abs(j) == 3) || (Math.abs(i) == 3 && Math.abs(j) == 2)) {
                    continue;
                }
                BlockPos p = center.offset(i, 0, j);
                if (!p.equals(pos)) {
                    if (!hatch.placeDummy(p)) {
                        level.destroyBlock(pos, true);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isRadResistant(Level level, BlockPos pos) {
        if (level != null && level.getBlockEntity(pos) instanceof IDoor door) {
            return door.getState() == IDoor.DoorState.CLOSED;
        }
        return true;
    }
}
