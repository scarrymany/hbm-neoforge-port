package com.hbm.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/**
 * Custom look-at HUD overlay contract, ported from CE. CE's {@code printGeneric} drew text with raw
 * {@code FontRenderer}/{@code GlStateManager} calls and a bespoke inline {@code &[...]&]} color-escape
 * format inside plain strings; neither has a modern equivalent now that tooltip/HUD text is
 * {@link Component}-based ({@link net.minecraft.network.chat.Style} carries color instead), so that
 * drawing code is not ported. Implementers hook {@link #printHook} against
 * {@link RenderGuiEvent.Pre} and draw with {@code GuiGraphics} directly; a shared helper can be
 * reintroduced once a concrete implementor's exact layout needs are known.
 */
public interface ILookOverlay {

    @OnlyIn(Dist.CLIENT)
    void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos);
}
