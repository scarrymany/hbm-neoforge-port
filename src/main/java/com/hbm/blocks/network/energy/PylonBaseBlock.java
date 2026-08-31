package com.hbm.blocks.network.energy;

import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import com.mojang.serialization.MapCodec;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.PylonBase} (abstract shared base for
 * {@link PylonRedWireBlock}, read in full): dye-to-set-color right click, break-time link teardown.
 * {@link PylonLargeBlock}/{@link com.hbm.blocks.network.PylonMediumBlock}/{@link SubstationBlock}
 * are NOT subclasses of this (CE's own class hierarchy: those three extend {@code BlockDummyable}
 * directly, only sharing {@code TileEntityPylonBase} at the tile-entity layer, same here) - each of
 * those three block classes ports the identical {@code breakBlock}/{@code onBlockActivated} bodies
 * itself, matching CE's own duplication instead of inventing a shared block-level mixin CE never had.
 *
 * <p>CE's block is invisible/non-solid ({@code isOpaqueCube}/{@code isNormalCube}/
 * {@code shouldSideBeRendered} all {@code false}, rendered instead as an animated TESR wire mesh -
 * {@code EnumBlockRenderType.ENTITYBLOCK_ANIMATED}). No wire renderer exists in this port yet (Phase
 * 5 client work); {@link #getShape} returns an empty collision/selection shape (matching CE's
 * non-solid contract) so players are not blocked or shown a placeholder cube hitbox in the meantime.
 */
public abstract class PylonBaseBlock extends BaseEntityBlock implements ITooltipProvider {

    public static final MapCodec<PylonBaseBlock> CODEC = simpleCodec(p -> { throw new UnsupportedOperationException("PylonBaseBlock is code-registered, not data-driven"); });

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    protected PylonBaseBlock(Properties properties) {
        super(properties);
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

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
            pylon.disconnectAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Ported from CE's {@code onBlockActivated} - dye right-click sets the wire's tint, sneaking passes through. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon && pylon.setColor(player.getMainHandItem())) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
