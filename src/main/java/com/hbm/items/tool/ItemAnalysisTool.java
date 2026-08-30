package com.hbm.items.tool;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.IAnalyzable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * {@link IAnalyzable}-driven debug tool, ported from CE's
 * {@code com.hbm.items.tool.ItemAnalysisTool}. Resolves {@link BlockDummyable} dummy positions to
 * their core first (matching CE's own {@code findCore} call), then dispatches to
 * {@link IAnalyzable#getDebugInfo} - both {@link BlockDummyable} and {@link IAnalyzable} are already
 * real, working infrastructure in this port ({@code com.hbm.blocks.network.FluidDuctBaseBlock}
 * already implements {@link IAnalyzable}, confirmed by direct read), so this item needed no new
 * interface work, only its own existence.
 */
public class ItemAnalysisTool extends Item {

    public ItemAnalysisTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null) {
                pos = core;
                block = level.getBlockState(pos).getBlock();
            }
        }

        if (block instanceof IAnalyzable analyzable) {
            List<String> debug = analyzable.getDebugInfo(level, pos);

            if (debug != null && !level.isClientSide && player != null) {
                for (String line : debug) {
                    player.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW), false);
                }
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
