package com.hbm.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * Opt-out-of-vanilla-outline, draw-your-own-highlight contract, ported from CE. CE's implementation
 * drew the box with raw {@code GlStateManager}/{@code GL11} immediate-mode calls, which have no
 * modern equivalent; that drawing code is not ported here and is left to whichever future
 * client-rendering area implements {@link #drawHighlight} against {@link RenderHighlightEvent.Block}
 * using the modern {@code PoseStack}/{@code VertexConsumer} pipeline.
 */
public interface ICustomBlockHighlight {

    @OnlyIn(Dist.CLIENT)
    boolean shouldDrawHighlight(Level level, BlockPos pos);

    @OnlyIn(Dist.CLIENT)
    void drawHighlight(RenderHighlightEvent.Block event, Level level, BlockPos pos);
}
