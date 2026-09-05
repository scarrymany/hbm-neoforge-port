package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.HeatBoilerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.trait.FT_Heatable;
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
import java.util.Locale;

/** CE {@code MachineHeatBoilerIndustrial} — Dummyable {4,0,1,1,1,1} offset 1. Held fluid-ID Exact CE {@code :61-88}. fillSpace extras Exact CE {@code :108-116}. printHook Exact CE {@code :119-153}. Tooltip Exact CE {@code :157-164}. */
public class HeatBoilerIndustrialBlock extends BlockDummyable implements ILookOverlay, ITooltipProvider {

    public HeatBoilerIndustrialBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? HeatBoilerBlockEntity.industrial(DummyableProcessBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_INDUSTRIAL_BOILER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Exact CE MachineHeatBoilerIndustrial.java:61-88 — !sneak + ID + FT_Heatable BOILER
        if (!player.isShiftKeyDown() && !stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof HeatBoilerBlockEntity boiler) {
                    var type = ident.getType(level, core, stack);
                    if (type.hasTrait(FT_Heatable.class)
                            && type.getTrait(FT_Heatable.class).getEfficiency(FT_Heatable.HeatingType.BOILER) > 0) {
                        boiler.water.setTankType(type);
                        boiler.setChanged();
                        player.displayClientMessage(Component.literal("Changed type to ")
                                .append(type.getLocalizedName())
                                .append(Component.literal("!"))
                                .withStyle(ChatFormatting.YELLOW), false);
                    }
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

    /**
     * Exact CE {@code MachineHeatBoilerIndustrial.fillSpace} extras ({@code MachineHeatBoilerIndustrial.java:108-116}).
     * After {@code super.fillSpace} (AABB only): four cardinal ports around {@code placed − dir}, plus the
     * top extra at {@code y+4}. CE uses the <em>placed</em> {@code x,y,z} and subtracts {@code dir} only —
     * it does <em>not</em> add {@code o}. HeatBoiler uses core-relative rot/−rot + {@code y+3}; industrial
     * uses axis-aligned ±1 + {@code y+4}.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos base = placedPos.relative(dir.getOpposite());
        makeExtra(level, base.offset(1, 0, 0));
        makeExtra(level, base.offset(-1, 0, 0));
        makeExtra(level, base.offset(0, 0, 1));
        makeExtra(level, base.offset(0, 0, -1));
        makeExtra(level, base.above(4));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineHeatBoilerIndustrial.java:119-153
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof HeatBoilerBlockEntity boiler)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(String.format(Locale.US, "%,d", boiler.heat) + "TU"));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(boiler.water.getTankType().getLocalizedName())
                        .append(Component.literal(": "
                                + String.format(Locale.US, "%,d", boiler.water.getFill())
                                + " / "
                                + String.format(Locale.US, "%,d", boiler.water.getMaxFill())
                                + "mB"))));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(boiler.steam.getTankType().getLocalizedName())
                        .append(Component.literal(": "
                                + String.format(Locale.US, "%,d", boiler.steam.getFill())
                                + " / "
                                + String.format(Locale.US, "%,d", boiler.steam.getMaxFill())
                                + "mB"))));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE MachineHeatBoilerIndustrial.java:157-164
        addStandardInfo(tooltip);
    }
}
