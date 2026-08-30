package com.hbm.items.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemCoordinateBase} (74 lines, read in full) - generic
 * "shift-right-click a block to save its coordinates onto this stack" base, per
 * {@code docs/phase3/scattered_military_items.md}'s Cluster 1. CE's package-only-consumer is
 * {@link ItemRadarLinker}; the class travels with it.
 * <p>
 * {@code onItemUse}/{@code EnumActionResult} maps onto {@link #useOn(UseOnContext)} exactly like
 * this port's already-working {@link ItemAnalysisTool#useOn} and {@link ItemDetonator#useOn} -
 * confirmed real NeoForge 1.21.1 shape, no new pattern needed.
 * <p>
 * The stored position uses {@link ToolDataComponents#DETONATOR_POS} - the same
 * {@code DataComponentType<BlockPos>} {@link ItemDetonator} already registered for its own
 * "shift-right-click a block to store its position" pattern. Per this task's own instruction to
 * reuse a shared component when the semantics match: CE's {@code ItemCoordinateBase} and CE's
 * {@code ItemDetonator} write the exact same {@code x}/{@code y}/{@code z} NBT-int-triplet shape for
 * the exact same purpose (remember a target block position for a later action), so this reuses that
 * field rather than declaring a second, functionally identical one. Flagged in this package's final
 * report for the review wave in case a reconciliation pass wants a more neutrally-named field.
 */
public abstract class ItemCoordinateBase extends Item {

    public ItemCoordinateBase(Properties properties) {
        super(properties);
    }

    public static BlockPos getPosition(ItemStack stack) {
        return stack.get(ToolDataComponents.DETONATOR_POS.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!this.canGrabCoordinateHere(level, pos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            BlockPos target = this.getCoordinates(level, pos);
            ItemStack stack = context.getItemInHand();
            stack.set(ToolDataComponents.DETONATOR_POS.get(), target);

            Player player = context.getPlayer();
            if (player != null) {
                this.onTargetSet(level, target, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether this position can be saved or if the position target is valid. */
    public abstract boolean canGrabCoordinateHere(Level level, BlockPos pos);

    /** Whether this linking item saves the Y coordinate (tooltip display only - see class javadoc). */
    public boolean includeY() {
        return true;
    }

    /** Modifies the saved coordinates, for example resolving a multiblock's core position. */
    public BlockPos getCoordinates(Level level, BlockPos pos) {
        return pos;
    }

    /** Extra behavior on a successful target set, e.g. sounds. */
    public void onTargetSet(Level level, BlockPos pos, Player player) {
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        BlockPos pos = getPosition(stack);
        if (pos != null) {
            list.add(Component.literal("X: " + pos.getX()));
            if (includeY()) list.add(Component.literal("Y: " + pos.getY()));
            list.add(Component.literal("Z: " + pos.getZ()));
        } else {
            list.add(Component.literal("No position set!").withStyle(ChatFormatting.RED));
        }
    }
}
