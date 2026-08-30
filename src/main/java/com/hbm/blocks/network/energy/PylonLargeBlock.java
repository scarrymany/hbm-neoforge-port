package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import com.hbm.blockentity.network.energy.PylonLargeBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
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
 * Ported from CE's {@code com.hbm.blocks.network.energy.PylonLarge} (read in full): the large-tier
 * multiblock pylon, {@code {13,0,1,1,1,1}} dimensions (a 13-block-tall single-column tower), offset
 * 0, {@code QUAD} connection type. Only the core (meta 12-15) carries a block entity - every dummy
 * position is a bare column segment, matching CE's own {@code createNewTileEntity} (returns
 * {@code null} below meta 12) and this port's now-established {@code BlockDummyable} convention (see
 * {@code MachineSteamEngineBlock}'s identical pattern).
 *
 * <p><b>Simplification vs. CE</b>: CE's {@code getMetaForCore} override recomputes the placement
 * facing from the player's yaw a second time, at double the base method's angular resolution
 * ({@code yaw*4/180} vs. {@link BlockDummyable#setPlacedBy}'s own {@code yaw*4/360} bucketing) before
 * folding it back to the same 4-way range - a cosmetic-orientation quirk unrelated to the network
 * graph this pass ports. Not overridden here: the base class's own yaw bucket is used directly,
 * still landing in the correct core-rotation meta range (12-15) that
 * {@link PylonLargeBlockEntity#getMountPos()} reads from.
 */
public class PylonLargeBlock extends BlockDummyable implements ITooltipProvider {

    public PylonLargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{13, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new PylonLargeBlockEntity(EnergyNetworkBlockEntities.PYLON_LARGE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.PYLON_LARGE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
            pylon.disconnectAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Ported from CE's {@code onBlockActivated} (dye right-click, resolved to the core). */
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
