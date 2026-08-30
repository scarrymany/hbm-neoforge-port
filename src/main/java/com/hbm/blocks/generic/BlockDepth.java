package com.hbm.blocks.generic;

import com.hbm.api.item.IDepthRockTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ported from CE's {@code BlockDepth}: the shared "world-context" stone base for the depth-stratum
 * blocks ({@code stone_depth}, {@code depth_brick}, ...) and, via {@link BlockDepthOre}, for the
 * depth ore/cluster family. Effectively unbreakable by hand; only tools implementing
 * {@link IDepthRockTool} can mine it at normal speed, matching CE's
 * {@code getPlayerRelativeBlockHardness} override (modern equivalent: {@link #getDestroyProgress}).
 */
public class BlockDepth extends Block {

    public BlockDepth(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof IDepthRockTool tool) {
            if (level instanceof Level realLevel && tool.canBreakRock(realLevel, player, held, state, pos)) {
                return 1.0F / 100.0F;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("trait.unmineable").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("trait.destroybyexplosion").withStyle(ChatFormatting.YELLOW));
        float resistance = this.getExplosionResistance();
        if (resistance > 50.0F) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }
    }
}
