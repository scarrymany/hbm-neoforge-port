package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.FusionKlystronCreativeBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.handler.MultiblockHandlerXR;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** CE {@code MachineFusionKlystronCreative} — same dims as klystron, no extras TE. */
public class MachineFusionKlystronCreativeBlock extends BlockDummyable implements ITooltipProvider {

    public MachineFusionKlystronCreativeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 4, 3, 2, 2};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new FusionKlystronCreativeBlockEntity(FusionBlockEntities.FUSION_KLYSTRON_CREATIVE.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.FUSION_KLYSTRON_CREATIVE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return super.checkRequirement(level, placedPos, dir, placementOffset)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, -3, 4, 3, 1, 1}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, placedPos.relative(dir, placementOffset), new int[]{4, -3, 4, 3, 1, 1}, this, dir);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
