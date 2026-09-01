package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.FusionBoilerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** CE {@code MachineFusionBoiler} Dummyable {3,0,4,4,1,1} offset 4. */
public class MachineFusionBoilerBlock extends BlockDummyable implements ILookOverlay, ITooltipProvider {

    public MachineFusionBoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 4, 4, 1, 1};
    }

    @Override
    public int getOffset() {
        return 4;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new FusionBoilerBlockEntity(FusionBlockEntities.FUSION_BOILER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.FUSION_BOILER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.offset(-dir.getStepX() + rot.getStepX(), 0, -dir.getStepZ() + rot.getStepZ()));
        makeExtra(level, core.offset(-dir.getStepX() - rot.getStepX(), 0, -dir.getStepZ() - rot.getStepZ()));
        makeExtra(level, core.offset(dir.getStepX() * 2 + rot.getStepX(), 0, dir.getStepZ() * 2 + rot.getStepZ()));
        makeExtra(level, core.offset(dir.getStepX() * 2 - rot.getStepX(), 0, dir.getStepZ() * 2 - rot.getStepZ()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getBlockEntity(core) instanceof FusionBoilerBlockEntity boiler)) return;
        List<Component> text = new ArrayList<>();
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(BobMathUtil.getShortNumber(boiler.plasmaEnergy) + " TU").withStyle(ChatFormatting.RESET)));
        List<FluidTankNTM> tanks = boiler.getAllTanks();
        for (int i = 0; i < tanks.size(); i++) {
            FluidTankNTM tank = tanks.get(i);
            text.add(Component.literal(i == 0 ? "-> " : "<- ").withStyle(i == 0 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(tank.getTankType().getLocalizedName())
                    .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB").withStyle(ChatFormatting.RESET)));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
