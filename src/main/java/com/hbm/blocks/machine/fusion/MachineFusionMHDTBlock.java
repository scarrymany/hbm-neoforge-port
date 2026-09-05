package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.FusionMHDTBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.handler.MultiblockHandlerXR;
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

/** CE {@code MachineFusionMHDT} Dummyable {2,0,6,7,2,2} offset 7 + XR extras. */
public class MachineFusionMHDTBlock extends BlockDummyable implements ILookOverlay, ITooltipProvider {

    public MachineFusionMHDTBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 6, 7, 2, 2};
    }

    @Override
    public int getOffset() {
        return 7;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new FusionMHDTBlockEntity(FusionBlockEntities.FUSION_MHDT.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.FUSION_MHDT.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return super.checkRequirement(level, placedPos, dir, placementOffset)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -2, 6, 2, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -2, -6, 7, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -2, -3, 5, 2, 2}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, -3, -3, 5, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, placedPos.relative(dir, placementOffset + 3),
                new int[]{1, 0, 0, 1, 3, 3}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -2, 6, 2, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -2, -6, 7, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -2, -3, 5, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, -3, -3, 5, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, placedPos.relative(dir, placementOffset + 3), new int[]{1, 0, 0, 1, 3, 3}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.offset(dir.getStepX() * 4 + rot.getStepX() * 3, 0, dir.getStepZ() * 4 + rot.getStepZ() * 3));
        makeExtra(level, core.offset(dir.getStepX() * 4 - rot.getStepX() * 3, 0, dir.getStepZ() * 4 - rot.getStepZ() * 3));
        makeExtra(level, core.offset(dir.getStepX() * 7, 1, dir.getStepZ() * 7));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getBlockEntity(core) instanceof FusionMHDTBlockEntity turbine)) return;
        boolean hasPlasma = turbine.hasMinimumPlasma();
        boolean isCool = turbine.isCool();
        long power = (long) Math.floor(turbine.plasmaEnergy * FusionMHDTBlockEntity.PLASMA_EFFICIENCY);
        if (!hasPlasma) power /= 2;
        List<Component> text = new ArrayList<>();
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(BobMathUtil.getShortNumber(turbine.plasmaEnergy) + "TU/t / "
                        + BobMathUtil.getShortNumber(FusionMHDTBlockEntity.MINIMUM_PLASMA) + "TU/t")
                        .withStyle(hasPlasma ? ChatFormatting.RESET : ChatFormatting.GOLD)));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.literal(BobMathUtil.getShortNumber(!isCool ? 0 : power) + "HE/t").withStyle(ChatFormatting.RESET)));
        List<FluidTankNTM> tanks = turbine.getAllTanks();
        for (int i = 0; i < tanks.size(); i++) {
            FluidTankNTM tank = tanks.get(i);
            text.add(Component.literal(i == 0 ? "-> " : "<- ").withStyle(i == 0 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(tank.getTankType().getLocalizedName())
                    .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB").withStyle(ChatFormatting.RESET)));
        }
        if (turbine.plasmaEnergy > 0 && !hasPlasma) {
            text.add(Component.literal("! LOW POWER !").withStyle(BobMathUtil.getBlink() ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        }
        if (!isCool) {
            text.add(Component.literal("! ! ! INSUFFICIENT COOLING ! ! !").withStyle(BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
