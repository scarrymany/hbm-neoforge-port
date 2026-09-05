package com.hbm.items.tool;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.interfaces.ICopiable;
import com.hbm.util.Either;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Exact CE {@code ItemSettingsTool}: sneak-copy / paste + {@code TOOL_ALT} {@code copyIndex} cycle
 * ({@code :46-95}/{@code :124-143}). {@code PlayerInformPacketLegacy} → action-bar
 * {@code displayClientMessage} (same substitute as {@link ItemRangefinder}).
 */
public class ItemSettingsTool extends Item {

    public ItemSettingsTool(Properties properties) {
        super(properties);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Exact CE ItemSettingsTool.java:41-43
        if (slotChanged) return true;
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Exact CE ItemSettingsTool.java:46-94
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        if (!isSelected || !TagsUtil.hasCustomData(stack)) return;
        if (player.getMainHandItem() != stack) return;

        CompoundTag tag = TagsUtil.getCustomData(stack);
        int delay = tag.getInt("inputDelay") + 1;
        ListTag displayInfo = tag.getList("displayInfo", Tag.TAG_COMPOUND);

        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);
        if (props.getKeyPressed(EnumKeybind.TOOL_ALT) && delay > 4) {
            int index = tag.getInt("copyIndex") + 1;
            if (index > displayInfo.size() - 1) {
                index = 0;
            }
            tag.putInt("copyIndex", index);
            delay = 0;
        }

        tag.putInt("inputDelay", delay);
        TagsUtil.putCustomData(stack, tag);

        if (level.getGameTime() % 5 != 0) return;
        if (displayInfo.isEmpty()) return;

        int copyIndex = tag.getInt("copyIndex");
        MutableComponent line = Component.empty();
        for (int j = 0; j < displayInfo.size(); j++) {
            if (j > 0) line.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            ChatFormatting format = copyIndex == j ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
            line.append(Component.translatable(displayInfo.getCompound(j).getString("info")).withStyle(format));
        }
        player.displayClientMessage(line, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        Either<BlockEntity, Block> source = getCopySource(level, pos);
        if (source == null) return InteractionResult.PASS;
        ICopiable copiable = source.isLeft() ? (ICopiable) source.left() : (ICopiable) source.right();

        if (level.isClientSide) return InteractionResult.PASS;
        if (player == null) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            CompoundTag settings = copiable.getSettings(level, pos);
            settings.putString("tileName", copiable.getSettingsSourceID(source));
            settings.putInt("copyIndex", 0);
            settings.putInt("inputDelay", 0);
            // Exact CE ItemSettingsTool.java:134-143
            String[] info = copiable.infoForDisplay(level, pos);
            if (info != null) {
                ListTag displayInfo = new ListTag();
                for (String str : info) {
                    CompoundTag infoTag = new CompoundTag();
                    infoTag.putString("info", str);
                    displayInfo.add(infoTag);
                }
                settings.put("displayInfo", displayInfo);
            }
            TagsUtil.putCustomData(stack, settings);
            player.displayClientMessage(Component.literal("Copied settings of " + copiable.getSettingsSourceDisplay(source)).withStyle(ChatFormatting.AQUA), true);
        } else if (TagsUtil.hasCustomData(stack)) {
            CompoundTag tag = TagsUtil.getCustomData(stack);
            copiable.pasteSettings(tag, tag.getInt("copyIndex"), level, player, pos);
        }

        return InteractionResult.SUCCESS;
    }

    private static Either<BlockEntity, Block> getCopySource(Level level, BlockPos pos) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof ICopiable) return Either.left(te);

        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof ICopiable) return Either.right(block);

        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Can copy the settings (filters, fluid ID, etc) of machines"));
        tooltip.add(Component.literal("Shift right-click to copy, right click to paste"));
        tooltip.add(Component.literal("Ctrl click on pipes to paste settings to multiple pipes"));

        if (TagsUtil.hasCustomData(stack)) {
            CompoundTag tag = TagsUtil.getCustomData(stack);
            if (tag.contains("tileName")) {
                tooltip.add(Component.translatable(tag.getString("tileName")).withStyle(ChatFormatting.BLUE));
            } else {
                tooltip.add(Component.literal(" None ").withStyle(ChatFormatting.RED));
            }
        }
    }
}
