package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.HeatBoilerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** CE {@code MachineHeatBoiler} — Dummyable {3,0,1,1,1,1} offset 1. Held fluid-ID Exact CE {@code :65-78}. fillSpace extras Exact CE {@code :115-126}. printHook Exact CE {@code :135-149}. Tooltip Exact CE {@code :129-132}. */
public class HeatBoilerBlock extends BlockDummyable implements ILookOverlay, ITooltipProvider {

    public HeatBoilerBlock(Properties properties) {
        super(properties);
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
        return state.getValue(META) >= 12
                ? HeatBoilerBlockEntity.small(DummyableProcessBlockEntities.HEAT_BOILER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.HEAT_BOILER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Exact CE MachineHeatBoiler.java:65-78 — !sneak + IItemFluidIdentifier → tanks[0]
        if (!player.isShiftKeyDown() && !stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof HeatBoilerBlockEntity boiler) {
                    var type = ident.getType(level, core, stack);
                    boiler.water.setTankType(type);
                    boiler.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to ")
                            .append(type.getLocalizedName())
                            .append(Component.literal("!"))
                            .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        // Exact CE MachineHeatBoiler.java:115-126 — side ports + top extra
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(rot));
        makeExtra(level, core.relative(rot.getOpposite()));
        makeExtra(level, core.above(3));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineHeatBoiler.java:135-149
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof HeatBoilerBlockEntity boiler)) return;

        List<Component> text = new ArrayList<>();
        FluidTankNTM[] tanks = {boiler.water, boiler.steam};
        for (int i = 0; i < tanks.length; i++) {
            FluidTankNTM tank = tanks[i];
            text.add(Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(tank.getTankType().getLocalizedName())
                            .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"))));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE MachineHeatBoiler.java:129-132
        addStandardInfo(tooltip);
    }
}
