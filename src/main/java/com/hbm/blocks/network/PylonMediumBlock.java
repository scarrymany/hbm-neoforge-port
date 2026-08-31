package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import com.hbm.blockentity.network.energy.PylonMediumBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities;
import net.minecraft.ChatFormatting;
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
 * Ported from CE's {@code com.hbm.blocks.network.PylonMedium} (read in full - lives directly under
 * {@code blocks/network}, not {@code blocks/network/energy}, matching the research report's own
 * inventory placement and preserved here for the same reason). {@code {6,0,0,0,0,0}} dimensions
 * (a 6-tall single-column tower, dummies straight up only), offset 0, {@code TRIPLE} connection type.
 */
public class PylonMediumBlock extends BlockDummyable implements ITooltipProvider {

    public PylonMediumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{6, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new PylonMediumBlockEntity(EnergyNetworkBlockEntities.PYLON_MEDIUM.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.PYLON_MEDIUM.get() ? ITickableBE.ticker() : null;
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
        tooltip.add(Component.literal("Connection Type: Triple").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Connection Range: 45m").withStyle(ChatFormatting.GOLD));
    }
}
