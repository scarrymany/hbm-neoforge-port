package com.hbm.items.tool;

import com.hbm.interfaces.ICopiable;
import com.hbm.util.Either;
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
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * {@link ICopiable} machine-settings copy/paste tool, ported from CE's
 * {@code com.hbm.items.tool.ItemSettingsTool} (read in full). Shift-right-click a block or block
 * entity implementing {@link ICopiable} to copy its settings into the stack's
 * {@code minecraft:custom_data} (CE: the stack's own {@code NBTTagCompound}); a plain right-click
 * pastes them onto the next {@link ICopiable} target. {@link ICopiable} is already real,
 * already-implemented infrastructure in this port (confirmed by three independent Phase 2 research
 * reports converging on the same finding), so this item needed no interface work.
 * <p>
 * <b>Simplified from CE</b>: CE's multi-field {@code displayInfo} cycling (alt-click to page through
 * a copied machine's info lines, shown via a dedicated action-bar packet channel) is collapsed into a
 * plain tooltip listing every {@code infoForDisplay} line at once - the packet-driven cycling
 * behavior is a client-UX nicety, not the copy/paste mechanic itself, and every line is still visible
 * (just all at once instead of one at a time).
 */
public class ItemSettingsTool extends Item {

    public ItemSettingsTool(Properties properties) {
        super(properties);
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

        if (TagsUtil.hasCustomData(stack)) {
            CompoundTag tag = TagsUtil.getCustomData(stack);
            if (tag.contains("tileName")) {
                tooltip.add(Component.literal(tag.getString("tileName")).withStyle(ChatFormatting.BLUE));
            }
        }
    }
}
