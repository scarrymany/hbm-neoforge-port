package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.ChimneyBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CE {@code MachineChimneyBrick}/{@code MachineChimneyIndustrial}.
 * Dummyable {12|22,0,1,1,1,1} offset 1. No GUI.
 */
public class MachineChimneyBlock extends BlockDummyable implements ITooltipProvider {

    private final boolean industrial;

    public MachineChimneyBlock(Properties properties, boolean industrial) {
        super(properties);
        this.industrial = industrial;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{industrial ? 22 : 12, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(META) < 12) return null;
        return industrial
                ? ChimneyBlockEntity.industrial(DummyableProcessBlockEntities.CHIMNEY_INDUSTRIAL.get(), pos, state)
                : ChimneyBlockEntity.brick(DummyableProcessBlockEntities.CHIMNEY_BRICK.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        var expected = industrial ? DummyableProcessBlockEntities.CHIMNEY_INDUSTRIAL.get()
                : DummyableProcessBlockEntities.CHIMNEY_BRICK.get();
        return type == expected ? ITickableBE.ticker() : null;
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        makeExtra(level, core.east());
        makeExtra(level, core.west());
        makeExtra(level, core.north());
        makeExtra(level, core.south());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
