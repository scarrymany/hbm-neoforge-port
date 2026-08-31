package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.network.ICachedPipeConnections;
import com.hbm.blockentity.network.PipeBaseBlockEntity;
import com.hbm.blocks.IAnalyzable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.MapCodec;

/**
 * Abstract base for the entire {@code FluidDuctBase} family (10 concrete blocks, 8 distinct block
 * entity classes - see {@code docs/phase2/network_fluid_ducts.md}'s registry table), ported from CE's
 * {@code com.hbm.blocks.network.FluidDuctBase extends BlockContainer implements IAnalyzable,
 * IBlockFluidDuct}. All 10 are simple {@code BaseEntityBlock}s with no multiblock footprint and no
 * GUI/menu (confirmed by the research report: neither {@code TileEntityFluidValve} nor
 * {@code TileEntityFluidCounterValve} implements {@code IGUIProvider}).
 *
 * <p>Owns {@link #changeTypeRecursively} (the cosmetic type-propagation flood fill, distinct from the
 * logical {@link FluidNetMK2} graph the block entities themselves join via
 * {@code com.hbm.uninos.UniNodespace} - see the research report's "two distinct mechanisms" section)
 * and {@link #getDebugInfo} (reads {@code FluidNetMK2.links}/{@code receiverEntries}/
 * {@code providerEntries}/{@code fluidTracker} for {@code ItemAnalyzer}, per CE's own
 * {@code FluidDuctBase.getDebugInfo}). Both walk only {@link PipeBaseBlockEntity} instances - CE's own
 * {@code instanceof TileEntityPipeBaseNT} check, which correctly skips the exhaust-duct family
 * ({@code TileEntityPipeExhaust} carries three fixed smoke types with no single settable
 * {@code FluidType}, and is not a {@code TileEntityPipeBaseNT} subclass in CE either).
 *
 * <p>Rendering (connection-aware collision boxes, dynamic/baked models, {@code IDynamicModels}) is
 * entirely deferred to Phase 5 per the research report's "Deferred scope" - every concrete subclass
 * here uses {@link RenderShape#MODEL} with no shape override, matching this port's other placeholder
 * Phase 2 blocks (e.g. {@code FluidTankBlock}) rather than porting CE's per-connection-mask AABB math
 * for a model that doesn't exist yet.
 */
public abstract class FluidDuctBaseBlock extends BaseEntityBlock implements IAnalyzable, IBlockFluidDuct {

    public static final MapCodec<FluidDuctBaseBlock> CODEC = simpleCodec(p -> { throw new UnsupportedOperationException("FluidDuctBaseBlock is code-registered, not data-driven"); });

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    protected FluidDuctBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ITickableBE.ticker();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        IPersistentNBT.restoreData(level, pos, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            IPersistentNBT.breakBlock(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Neighbor-block-update hook every duct subclass wires (matches CE's own
     * {@code FluidDuctStandard}/{@code FluidDuctBox} {@code neighborChanged} overrides) so a duct's
     * connection-mask cache invalidates as soon as a neighboring block changes.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof ICachedPipeConnections cached) {
            cached.invalidateConnectionCache();
        }
    }

    @Override
    public void changeTypeRecursively(Level level, BlockPos pos, FluidType prevType, FluidType type, int loopsRemaining) {
        if (!(level.getBlockEntity(pos) instanceof PipeBaseBlockEntity pipe)) return;
        if (pipe.getType() != prevType || pipe.getType() == type) return;

        pipe.setType(type);
        if (loopsRemaining <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).getBlock() instanceof IBlockFluidDuct duct) {
                duct.changeTypeRecursively(level, neighbor, prevType, type, loopsRemaining - 1);
            }
        }
    }

    @Override
    public List<String> getDebugInfo(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PipeBaseBlockEntity pipe)) return null;

        FluidType type = pipe.getType();
        if (type == null) return null;

        FluidNode node = (FluidNode) UniNodespace.getNode(level, pos, type.getNetworkProvider());
        if (node == null || node.net == null) return null;

        FluidNetMK2 net = node.net;
        List<String> debug = new ArrayList<>();
        debug.add("Links: " + net.links.size());
        debug.add("Subscribers: " + net.receiverEntries.size());
        debug.add("Providers: " + net.providerEntries.size());
        debug.add("Transfer: " + net.fluidTracker);
        return debug;
    }
}
