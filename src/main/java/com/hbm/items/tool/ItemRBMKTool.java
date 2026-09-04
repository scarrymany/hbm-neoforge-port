package com.hbm.items.tool;

import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKDisplayBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.blocks.machine.rbmk.RBMKConsoleBlock;
import com.hbm.blocks.machine.rbmk.RBMKDisplayBlock;
import com.hbm.util.TagsUtil;
import com.hbm.util.i18n.I18nUtil;
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
 * Exact CE {@code com.hbm.items.tool.ItemRBMKTool} {@code :43-120}: column → NBT
 * {@code posX/Y/Z}, console {@code setTarget}, display {@code setTarget}. Crane console
 * stays skipped (block/TE not registered). CE {@code getAttributeModifiers} +2 is a no-op
 * (returns {@code super} after putting the modifier) and is not reproduced.
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

        // CE ItemRBMKTool.java:46-61
        if (block instanceof RBMKBaseBlock) {
            BlockPos core = ((BlockDummyable) block).findCore(level, pos);
            if (core != null && !level.isClientSide) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                tag.putInt("posX", core.getX());
                tag.putInt("posY", core.getY());
                tag.putInt("posZ", core.getZ());
                TagsUtil.putCustomData(stack, tag);
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.rbmk_tool.linked").withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }

        // CE ItemRBMKTool.java:64-80 — port console is a single block, not Dummyable
        if (block instanceof RBMKConsoleBlock && TagsUtil.hasCustomData(stack)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof RBMKConsoleBlockEntity console && player != null) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                console.setTarget(tag.getInt("posX"), tag.getInt("posY"), tag.getInt("posZ"));
                player.sendSystemMessage(Component.translatable("item.rbmk_tool.set").withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }

        // CE ItemRBMKTool.java:99-112
        if (block instanceof RBMKDisplayBlock && TagsUtil.hasCustomData(stack)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof RBMKDisplayBlockEntity display && player != null) {
                CompoundTag tag = TagsUtil.getCustomData(stack);
                display.setTarget(tag.getInt("posX"), tag.getInt("posY"), tag.getInt("posZ"));
                player.sendSystemMessage(Component.translatable("item.rbmk_tool.set").withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // CE ItemRBMKTool.java:118-120
        for (String s : I18nUtil.resolveKeyArray("item.rbmk_tool.desc")) {
            tooltip.add(Component.literal(s).withStyle(ChatFormatting.YELLOW));
        }
    }
}
