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

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-target remote detonator, ported from CE's {@code com.hbm.items.tool.ItemMultiDetonator}
 * (read in full). Same store/fire split as {@link ItemDetonator}, generalized to a list:
 *
 * <ul>
 *   <li><b>Store</b> ({@link #useOn}, sneak-right-click-on-block): appends one more
 *   {@link BlockPos} to the stored list. No cap, no dedupe - matches CE's
 *   {@code ArrayUtils.add}-based parallel-array append exactly.</li>
 *   <li><b>Fire</b> ({@link #use}, plain right-click): detonates every stored position that
 *   resolves to an {@link IBomb}, reporting a "succeeded/total" count.</li>
 *   <li><b>Clear</b> ({@link #use}, sneak + not looking at a block - i.e. the same {@code use}
 *   entry point CE's {@code onItemRightClick} falls through to when its own {@code onItemUse}
 *   didn't consume the click): wipes the stored list back to empty.</li>
 * </ul>
 *
 * <p>CE's three parallel {@code xValues}/{@code yValues}/{@code zValues} int-array NBT fields
 * collapse onto one {@code List<BlockPos>} component ({@link ToolDataComponents#MULTI_DETONATOR_POS}).
 * Same "zero validation" CE parity note as {@link ItemDetonator} applies here unchanged.
 */
public class ItemMultiDetonator extends Item {

    public ItemMultiDetonator(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.callmultdet1")));
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.calldet2")));
        tooltip.add(Component.literal(I18nUtil.resolveKey("desc.callmultdet2")));

        List<BlockPos> locations = stack.getOrDefault(ToolDataComponents.MULTI_DETONATOR_POS.get(), List.of());
        if (locations.isEmpty()) {
            tooltip.add(Component.literal(I18nUtil.resolveKey("chat.posnoset")).withStyle(ChatFormatting.YELLOW));
        } else {
            for (int i = 0; i < locations.size(); i++) {
                BlockPos pos = locations.get(i);
                tooltip.add(Component.literal(
                        I18nUtil.resolveKey("chat.possetaxyz", i + 1, pos.getX(), pos.getY(), pos.getZ())).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /** CE: {@code onItemUse} - sneak-right-click on a block appends its position to the list. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        List<BlockPos> current = stack.getOrDefault(ToolDataComponents.MULTI_DETONATOR_POS.get(), List.of());
        List<BlockPos> updated = new ArrayList<>(current);
        updated.add(pos);
        stack.set(ToolDataComponents.MULTI_DETONATOR_POS.get(), List.copyOf(updated));

        Level level = context.getLevel();
        if (level.isClientSide()) {
            player.displayClientMessage(Component.literal("[" + I18nUtil.resolveKey("chat.posadd") + "]")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.AMBIENT, 2.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }

    /** CE: {@code onItemRightClick} - plain right-click fires every stored bomb; sneak-right-click (in air) clears the list. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<BlockPos> locations = stack.getOrDefault(ToolDataComponents.MULTI_DETONATOR_POS.get(), List.of());

        if (locations.isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.posnoseterror"))
                        .withStyle(ChatFormatting.RED), false);
            }
        } else if (!player.isShiftKeyDown()) {
            int succeeded = 0;

            for (BlockPos pos : locations) {
                if (level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);

                    if (!level.isClientSide()) {
                        bomb.explode(level, pos, player);

                        if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                            MainRegistry.logger.info("[DET] Tried to detonate block at {} / {} / {} by {}!",
                                    pos.getX(), pos.getY(), pos.getZ(), player.getDisplayName().getString());
                        }
                        succeeded++;
                    }
                }
            }

            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.detonatedmulti", succeeded, locations.size()))
                        .withStyle(ChatFormatting.DARK_GREEN), false);
            }
        } else {
            stack.set(ToolDataComponents.MULTI_DETONATOR_POS.get(), List.of());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.AMBIENT, 2.0F, 1.0F);

            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.removdedallpos"))
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }

        return super.use(level, player, hand);
    }
}
