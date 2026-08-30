package com.hbm.items.tool;

import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.blocks.machine.rbmk.RBMKConsoleBlock;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * RBMK console-linking tool, ported from CE's {@code com.hbm.items.tool.ItemRBMKTool} (read in
 * full). Right-click any {@link RBMKBaseBlock} column to mark its core position (via
 * {@link TagsUtil}), then right-click a {@link RBMKConsoleBlock} to feed that position into
 * {@link RBMKConsoleBlockEntity#setTarget} - both classes are real, already-shipped column-blocks
 * package content per {@code docs/phase2/rbmk_reactor.md}.
 * <p>
 * <b>Not ported</b>: CE's {@code rbmk_crane_console}/{@code rbmk_display} branches - neither
 * {@code TileEntityRBMKCraneConsole} nor {@code TileEntityRBMKDisplay} exists anywhere in this port
 * yet (confirmed: {@code com.hbm.blockentity.machine.rbmk} has no such classes), so those two
 * branches are genuinely unported targets, not an oversight in this item.
 */
public class ItemRBMKTool extends Item {

    public ItemRBMKTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Block block = level.getBlockState(pos).getBlock();

        if (block instanceof RBMKBaseBlock) {
            BlockPos core = ((BlockDummyable) block).findCore(level, pos);
            if (core != null && !level.isClientSide) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                tag.putInt("posX", core.getX());
                tag.putInt("posY", core.getY());
                tag.putInt("posZ", core.getZ());
                TagsUtil.putCustomData(stack, tag);
                if (player != null) {
                    player.displayClientMessage(Component.literal("Position linked").withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (block instanceof RBMKConsoleBlock && TagsUtil.hasCustomData(stack)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof RBMKConsoleBlockEntity console && player != null) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                console.setTarget(tag.getInt("posX"), tag.getInt("posY"), tag.getInt("posZ"));
                player.displayClientMessage(Component.literal("Target set").withStyle(ChatFormatting.YELLOW), false);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a column, then an RBMK console to link them."));
    }
}
