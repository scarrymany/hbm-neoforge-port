package com.hbm.items.special;

import com.hbm.config.GeneralConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.items.tool.ToolDataComponents;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.special.ItemDrop} (244 lines, read in full) - just the
 * {@code detonator_deadman} branch, per {@code docs/phase3/scattered_military_items.md}'s explicit
 * split recommendation (every branch in CE's monolithic class is an {@code this ==
 * ModItems.<field>} identity check with no shared logic, so the split is clean along item-identity
 * lines). Sneak-right-click a block to remember its position; dropping the item detonates whatever
 * {@link IBomb} sits there (if any) and always triggers a small local blast at the drop point.
 * <p>
 * Reuses {@link ToolDataComponents#DETONATOR_POS} rather than declaring a second, functionally
 * identical {@code DataComponentType<BlockPos>} - see that field's own javadoc and
 * {@link com.hbm.items.tool.ItemCoordinateBase}'s javadoc for the same reasoning; flagged in this
 * package's final report for the review wave in case a reconciliation pass wants a more neutrally
 * named shared field.
 * <p>
 * <b>Not reproduced</b>: CE credits the throwing player via {@code EntityItem#getThrower()}
 * (username lookup against the server's player list) when calling {@code IBomb.explode} - this has
 * no confirmed 1.21 equivalent, the same documented gap already accepted by this port's own
 * {@code com.hbm.hazard.type.HazardTypeUnstable} for an identical drop-detonation case. The
 * detonation itself still happens; only the credited detonator entity is {@code null}.
 */
public class ItemDeadmanDetonator extends Item {

    public ItemDeadmanDetonator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        stack.set(ToolDataComponents.DETONATOR_POS.get(), pos);

        Level level = context.getLevel();
        if (level.isClientSide()) {
            player.displayClientMessage(Component.translatable("chat.posset"), false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 2.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        Level level = entityItem.level();

        if (!level.isClientSide()) {
            BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
            if (pos != null && level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
                // See class javadoc: no credited detonator entity, matching HazardTypeUnstable's
                // identical documented gap.
                bomb.explode(level, pos, null);

                if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                    MainRegistry.logger.info("[DET] Tried to detonate block at {} / {} / {} by dead man's switch!",
                            pos.getX(), pos.getY(), pos.getZ());
                }
            }

            level.explode(entityItem, entityItem.getX(), entityItem.getY(), entityItem.getZ(), 0.0F, true, Level.ExplosionInteraction.NONE);
        }

        entityItem.discard();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Shift right-click to set position,"));
        tooltip.add(Component.literal("drop to detonate!"));

        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        if (pos == null) {
            tooltip.add(Component.literal("No position set!"));
        } else {
            tooltip.add(Component.literal("Set pos to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        }
        tooltip.add(Component.literal("[" + I18nUtil.resolveKey("trait.drop") + "]").withStyle(ChatFormatting.RED));
    }
}
