package com.hbm.blocks.bomb;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.bomb.NukeCasingBlockEntity;
import com.hbm.interfaces.IBomb;
import com.hbm.lib.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * Shared casing shell for the 9 concrete {@code Nuke*Block}s + {@code NukeCustomBlock}, ported from
 * the common structure every {@code Nuke*} block class in CE shares (each read individually in full -
 * see {@code docs/phase3/bomb_blocks_and_detonators.md} Section B): a {@code BlockHorizontal.FACING}-
 * backed (or, for {@code NukeMan}/{@code NukeFleija}, a derived-from-yaw) 6-facing casing implementing
 * {@link IBomb}, opening its GUI on a non-sneak click, exploding on redstone power, and dropping its
 * inventory when broken.
 * <p>
 * <b>Generalized without changing behavior</b>: CE's {@code neighborChanged} and {@code explode()}
 * are byte-for-byte identical bodies in all 9 classes read (ready-check, clear slots, remove block,
 * play sound, spawn explosion entity) except that {@code neighborChanged} passes a {@code null}
 * detonator and discards the return code - confirmed by direct comparison of every {@code Nuke*.java}
 * file's two methods. Rather than duplicate that body per concrete class, this base's
 * {@link #neighborChanged} simply calls {@link #explode(Level, BlockPos, net.minecraft.world.entity.Entity)}
 * with a {@code null} detonator, which is exactly what CE's own duplicated code already did.
 * <p>
 * {@code NukeBalefireBlock} still extends this class (its facing/GUI-open/redstone-triggers-explode
 * shell is identical to CE's own {@code NukeBalefire.neighborChanged}), but its block entity does
 * NOT extend {@link NukeCasingBlockEntity} - CE's {@code TileEntityNukeBalefire} is a
 * countdown-tickable {@code TileEntityMachineBase} subclass, closer in shape to a Phase 2 machine
 * than to the other 8 flat-check casings; see that class's own javadoc. {@link #onRemove} is
 * therefore checked against {@link MachineBaseBlockEntity} (the common ancestor), not
 * {@link NukeCasingBlockEntity}, so both shapes drop their inventory through this one override.
 */
public abstract class NukeCasingBlockBase extends BaseEntityBlock implements IBomb {

    public static final MapCodec<NukeCasingBlockBase> CODEC = simpleCodec(p -> { throw new UnsupportedOperationException("NukeCasingBlockBase is code-registered, not data-driven"); });

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected NukeCasingBlockBase(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Matches 7 of the 9 casings ({@code NukeBoy}/{@code Gadget}/{@code Mike}/{@code Tsar}/
     * {@code N2}/{@code Prototype}/{@code Custom}): {@code placer.getHorizontalFacing().getOpposite()}.
     * {@code NukeManBlock}/{@code NukeFleijaBlock} override this - see their own javadoc for the
     * derived-from-yaw facing CE used instead.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NukeCasingBlockEntity be) {
            be.placerID = serverPlayer.getUUID();
        }
    }

    /** CE: {@code onBlockActivated} - sneaking does nothing, a plain click opens the casing's GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            player.openMenu(menuProvider, pos);
        }
        return InteractionResult.CONSUME;
    }

    /** CE: {@code neighborChanged} - redstone-triggered detonation, detonator {@code null} (falls back to {@code placerID}). See class javadoc for why this calls {@link #explode} instead of duplicating each casing's ready-check/clear/spawn body. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            this.explode(level, pos, null);
        }
    }

    /**
     * CE: {@code breakBlock} - drops the casing's own inventory contents (component items not yet
     * consumed). Checked against {@link MachineBaseBlockEntity} rather than {@link NukeCasingBlockEntity}
     * so {@code NukeBalefireBlock} (whose block entity does not extend {@code NukeCasingBlockEntity},
     * see that class's javadoc) still gets its inventory dropped through this same shared override.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MachineBaseBlockEntity be) {
            InventoryHelper.dropInventoryItems(level, pos, be.inventory);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
