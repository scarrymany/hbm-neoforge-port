package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.ProxyConductorBlockEntity;
import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import com.hbm.blockentity.network.energy.SubstationBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.Substation} (read in full):
 * {@code {4,0,1,1,2,2}} dimensions, offset 1, {@code QUAD} connection type. Core (meta 12-15) carries
 * {@link SubstationBlockEntity}; the 4 diagonal "extra"-flagged corners {@link #fillSpace} marks
 * carry {@link ProxyConductorBlockEntity} (meta 6-11, {@link #hasExtra}); every other dummy position
 * carries no block entity, matching CE's own {@code createNewTileEntity} three-way branch.
 */
public class SubstationBlock extends BlockDummyable implements ITooltipProvider {

    public SubstationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        if (meta >= 12) return new SubstationBlockEntity(EnergyNetworkBlockEntities.SUBSTATION.get(), pos, state);
        if (hasExtra(meta)) return new ProxyConductorBlockEntity(EnergyNetworkBlockEntities.PROXY_CONDUCTOR.get(), pos, state);
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == EnergyNetworkBlockEntities.SUBSTATION.get() || type == EnergyNetworkBlockEntities.PROXY_CONDUCTOR.get()) {
            return ITickableBE.ticker();
        }
        return null;
    }

    /**
     * Ported from CE's own {@code fillSpace} override: on top of the base dummy fill, flags the 4
     * cells diagonally adjacent to the core (world-space {@code ±1,±1}, not rotated by {@code dir} -
     * matching CE's own hard-coded offsets and {@link SubstationBlockEntity#createNode()}'s identical
     * hard-coded corner math) as "extra" so {@link #newBlockEntity} gives them a
     * {@link ProxyConductorBlockEntity}.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);

        BlockPos core = placedPos.relative(dir, placementOffset);
        makeExtra(level, core.offset(1, 0, 1));
        makeExtra(level, core.offset(1, 0, -1));
        makeExtra(level, core.offset(-1, 0, 1));
        makeExtra(level, core.offset(-1, 0, -1));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
            pylon.disconnectAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockPos corePos = findCore(level, pos);
        if (corePos != null && level.getBlockEntity(corePos) instanceof PylonBaseBlockEntity pylon
                && pylon.setColor(player.getMainHandItem())) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
