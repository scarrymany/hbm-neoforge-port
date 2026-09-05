package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.FusionCouplerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** CE {@code MachineFusionCoupler} Dummyable {3,0,1,1,1,1} offset 0. */
public class MachineFusionCouplerBlock extends BlockDummyable implements ITooltipProvider {

    public MachineFusionCouplerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new FusionCouplerBlockEntity(FusionBlockEntities.FUSION_COUPLER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.FUSION_COUPLER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
