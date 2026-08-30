package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.List;

/**
 * Ported from CE's {@code Guide}: a simple facing waypoint/guide-post block with a joke tooltip.
 * CE hand-rolled the {@code FACING} property, {@code getStateForPlacement} and
 * {@code withRotation}/{@code withMirror}; the modern equivalent base class,
 * {@link HorizontalDirectionalBlock}, already provides the {@code FACING} property plus rotation/
 * mirror handling (via {@link #rotate}/{@link #mirror}), so only the state-definition wiring,
 * placement-facing and the tooltip need porting.
 */
public class Guide extends HorizontalDirectionalBlock {

    public static final MapCodec<Guide> CODEC = simpleCodec(Guide::new);

    public Guide(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Guide> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("\"Yeah no I think the wiki has details on that\""));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
