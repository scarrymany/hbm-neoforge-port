package com.hbm.items.tool;

import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.BlockConveyorBendable;
import com.hbm.blocks.network.ConveyorBlocks;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Conveyor-belt placement wand. Exact CE sneak-place
 * {@code ItemConveyorWand.java:218-251}: convert straight regular conveyor → lift/chute,
 * then stack lift-on-lift / chute-on-chute. Drag-path {@code WorldInAJar} stay skipped.
 */
public class ItemConveyorWand extends Item {

    private final Supplier<? extends Block> conveyorBlock;

    public ItemConveyorWand(Properties properties, Supplier<? extends Block> conveyorBlock) {
        super(properties);
        this.conveyorBlock = conveyorBlock;
    }

    private boolean hasSnakesAndLadders() {
        // Exact CE ItemConveyorWand.java:78-79 — REGULAR only
        return this.conveyorBlock.get() == ConveyorBlocks.CONVEYOR.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Direction facing = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        BlockState onState = level.getBlockState(pos);
        Block onBlock = onState.getBlock();

        // Exact CE ItemConveyorWand.java:224-233
        if (hasSnakesAndLadders() && onBlock == ConveyorBlocks.CONVEYOR.get()
                && onState.hasProperty(BlockConveyorBendable.CURVE)
                && onState.getValue(BlockConveyorBendable.CURVE) == BlockConveyorBendable.CurveType.STRAIGHT) {
            Direction beltFacing = onState.getValue(BlockConveyorBase.FACING);
            if (facing == Direction.UP) {
                if (!level.isClientSide) {
                    level.setBlock(pos, ConveyorBlocks.CONVEYOR_LIFT.get().defaultBlockState()
                            .setValue(BlockConveyorBase.FACING, beltFacing), 3);
                }
                return InteractionResult.SUCCESS;
            }
            if (facing == Direction.DOWN) {
                if (!level.isClientSide) {
                    level.setBlock(pos, ConveyorBlocks.CONVEYOR_CHUTE.get().defaultBlockState()
                            .setValue(BlockConveyorBase.FACING, beltFacing), 3);
                }
                return InteractionResult.SUCCESS;
            }
        }

        Block toPlace = this.conveyorBlock.get();
        // Exact CE ItemConveyorWand.java:237-239
        if (hasSnakesAndLadders()) {
            if (onBlock == ConveyorBlocks.CONVEYOR_LIFT.get() && facing == Direction.UP) {
                toPlace = ConveyorBlocks.CONVEYOR_LIFT.get();
            }
            if (onBlock == ConveyorBlocks.CONVEYOR_CHUTE.get() && facing == Direction.DOWN) {
                toPlace = ConveyorBlocks.CONVEYOR_CHUTE.get();
            }
        }

        BlockPos placePos = pos.relative(facing);
        if (!level.getBlockState(placePos).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockState defaultState = toPlace.defaultBlockState();
        BlockState placeState = defaultState.hasProperty(BlockConveyorBase.FACING)
                ? defaultState.setValue(BlockConveyorBase.FACING, player.getDirection())
                : defaultState;
        level.setBlock(placePos, placeState, 11);

        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE ItemConveyorWand.java:203-212
        if (Screen.hasShiftDown()) {
            for (String line : I18nUtil.resolveKeyArray("item.hbm.conveyor_wand.desc")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
            }
            if (hasSnakesAndLadders()) {
                tooltip.add(Component.literal(I18nUtil.resolveKey("item.conveyor_wand.vertical.desc"))
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            tooltip.add(Component.literal(I18nUtil.resolveKey("desc.misc.lshift")));
        }
    }
}
