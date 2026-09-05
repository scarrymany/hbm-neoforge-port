package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachinePumpBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code MachinePump} — Dummyable {3,0,1,1,1,1} offset 1. No GUI (ILookOverlay only).
 */
public class MachinePumpBlock extends BlockDummyable implements ITooltipProvider, ILookOverlay {

    private final boolean electric;

    public MachinePumpBlock(Properties properties, boolean electric) {
        super(properties);
        this.electric = electric;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(META) < 12) return null;
        return electric
                ? MachinePumpBlockEntity.electric(DummyableProcessBlockEntities.PUMP_ELECTRIC.get(), pos, state)
                : MachinePumpBlockEntity.steam(DummyableProcessBlockEntities.PUMP_STEAM.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        var expected = electric ? DummyableProcessBlockEntities.PUMP_ELECTRIC.get()
                : DummyableProcessBlockEntities.PUMP_STEAM.get();
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachinePumpBlockEntity pump)) return;

        List<Component> text = new ArrayList<>();
        if (pump.steam != null) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(pump.steam.getTankType().getLocalizedName())
                    .append(Component.literal(String.format(": %,d / %,d mB", pump.steam.getFill(), pump.steam.getMaxFill()))));
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(pump.lps.getTankType().getLocalizedName())
                    .append(Component.literal(String.format(": %,d / %,d mB", pump.lps.getFill(), pump.lps.getMaxFill()))));
        } else {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(String.format("%,d / %,d HE", pump.power, pump.getMaxPower()))));
        }
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(pump.water.getTankType().getLocalizedName())
                .append(Component.literal(String.format(": %,d / %,d mB", pump.water.getFill(), pump.water.getMaxFill()))));
        if (core.getY() > MachinePumpBlockEntity.groundHeight) {
            text.add(Component.literal("! ! ! ALTITUDE ! ! !").withStyle(ChatFormatting.RED));
        }
        if (!pump.onGround) {
            text.add(Component.literal("! ! ! NO VALID GROUND ! ! !").withStyle(ChatFormatting.RED));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
