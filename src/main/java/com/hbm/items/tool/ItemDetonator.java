package com.hbm.items.tool;

import com.hbm.config.GeneralConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Single-target remote detonator, ported from CE's {@code com.hbm.items.tool.ItemDetonator} (read
 * in full). CE's {@code onItemUse}/{@code onItemRightClick} pair maps 1:1 onto vanilla's
 * {@link #useOn(UseOnContext)} (sneak-right-click-on-block: store {@link BlockPos}) and
 * {@link #use(Level, Player, InteractionHand)} (plain right-click: detonate the stored position if
 * it resolves to an {@link IBomb}), exactly like this port's already-committed
 * {@link ItemTooling#useOn}/CE-derived split - confirmed real NeoForge 1.21.1 shape per
 * {@code docs/phase3/bomb_blocks_and_detonators.md}.
 *
 * <p>Stores the target position via {@link ToolDataComponents#DETONATOR_POS} instead of CE's raw
 * {@code x}/{@code y}/{@code z} NBT ints - vanilla {@link BlockPos} ships its own codec/stream
 * codec, so no bespoke component is needed.
 *
 * <p><b>Preserves CE's exact "no validation" behavior on purpose</b>: no range check, no dimension
 * check, no loaded-chunk check anywhere in this class, confirmed by reading CE's real source. A
 * stored position that no longer resolves to an {@link IBomb} (out of range, unloaded, wrong
 * dimension, or simply never was a bomb) silently falls into the generic "can't detonate" branch -
 * this is CE's real, intentional behavior, not a bug this port fixes.
 */
public class ItemDetonator extends Item {

    public ItemDetonator(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.calldet1")));
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.calldet2")));

        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        if (pos == null) {
            tooltip.add(Component.literal(I18nUtil.resolveKey("chat.posnoset")).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal(I18nUtil.resolveKey("chat.possetxyz", pos.getX(), pos.getY(), pos.getZ())).withStyle(ChatFormatting.GREEN));
        }
    }

    /** CE: {@code onItemUse} - sneak-right-click on a block stores its position. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        stack.set(ToolDataComponents.DETONATOR_POS.get(), pos);

        Level level = context.getLevel();
        if (level.isClientSide()) {
            player.displayClientMessage(Component.literal("[" + I18nUtil.resolveKey("chat.posset") + "]")
                    .withStyle(ChatFormatting.DARK_GREEN), false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.AMBIENT, 1.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }

    /** CE: {@code onItemRightClick} - plain right-click detonates the stored position. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());

        if (pos == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.posnoseterror"))
                        .withStyle(ChatFormatting.RED), false);
            }
        } else if (level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);

            if (!level.isClientSide()) {
                bomb.explode(level, pos, player);

                if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                    MainRegistry.logger.info("[DET] Tried to detonate block at {} / {} / {} by {}!",
                            pos.getX(), pos.getY(), pos.getZ(), player.getDisplayName().getString());
                }
                player.displayClientMessage(Component.literal("[" + I18nUtil.resolveKey("chat.detonated") + "]")
                        .withStyle(ChatFormatting.DARK_GREEN), false);
            }
        } else {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.postoofarerror"))
                        .withStyle(ChatFormatting.RED), false);
            }
        }

        return super.use(level, player, hand);
    }
}
