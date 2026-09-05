package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.CableDiodeBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.CableDiode} (read in full; block half only -
 * the TE lives in {@link CableDiodeBlockEntity}, see that class's javadoc for GUI deferred-scope
 * notes). CE's own {@code CONNECTION_MASK} render-only {@code IUnlistedProperty} is dropped outright
 * (no baked model exists to consume it in this port yet, and it has zero effect on the diode's
 * energy logic - unlike {@link BlockCable}'s mask, which also drives collision shape).
 * printHook Exact CE {@code CableDiode.java:144-155}. addInformation Exact CE {@code :134-136}.
 * GUI stay skipped.
 */
public class CableDiodeBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<CableDiodeBlock> CODEC = simpleCodec(CableDiodeBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public CableDiodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableDiodeBlockEntity(EnergyNetworkBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.CABLE_DIODE.get() ? ITickableBE.ticker() : null;
    }

    /**
     * CE opens {@code GUIDiode} here (limit/priority screen). No GUI framework port exists for this
     * screen yet (see {@link CableDiodeBlockEntity}'s javadoc) - sneaking still passes through
     * unchanged, matching CE; a non-sneaking click currently does nothing rather than opening a menu.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE CableDiode.java:134-136
        tooltip.add(Component.literal("Limits throughput and restricts flow direction")
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE CableDiode.java:144-155
        if (!(world.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Max.: " + BobMathUtil.getShortNumber(diode.getMaxPower()) + "HE/t"));
        text.add(Component.literal("Priority: " + diode.priority.name()));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
