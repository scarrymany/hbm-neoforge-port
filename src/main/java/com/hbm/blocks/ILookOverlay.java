package com.hbm.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
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
 * drawing code is not ported as-is. Implementers hook {@link #printHook} against
 * {@link RenderGuiEvent.Pre} and draw with {@code GuiGraphics} directly.
 * <p>
 * {@link #printGeneric} is the shared layout helper this interface's own javadoc previously deferred
 * ("reintroduced once a concrete implementor's exact layout needs are known") - added now that
 * {@code com.hbm.blocks.network.CraneSplitter} is the first real implementor (Phase 2 conveyor/crane
 * package), ported verbatim from Neo Edition's own confirmed-real, currently-running
 * {@code com.hbm.blocks.ILookOverlay#printGeneric} (same class/method name, package, and body -
 * confirmed API shape, not merely inferred).
 * <p>
 * Wired by {@code com.hbm.render.hud.LookOverlayDispatcher} — CE
 * {@code ModEventHandlerClient.java:839-865} (CROSSHAIRS + {@code DODD_RBMK_DIAGNOSTIC}).
 */
public interface ILookOverlay {

    @OnlyIn(Dist.CLIENT)
    void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos);

    @OnlyIn(Dist.CLIENT)
    static void printGeneric(RenderGuiEvent.Pre event, Component title, int titleCol, int bgCol, List<Component> text) {
        Minecraft mc = Minecraft.getInstance();

        Options options = mc.options;
        if (!options.getCameraType().isFirstPerson()) return;
        if (options.hideGui) return;
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;

        Font font = mc.font;

        event.getGuiGraphics().drawString(font, title.getString(), pX + 1, pZ - 9, bgCol, false);
        event.getGuiGraphics().drawString(font, title.getString(), pX, pZ - 10, titleCol, false);

        for (Component c : text) {
            event.getGuiGraphics().drawString(font, c, pX, pZ, 0xFFFFFF);
            pZ += 10;
        }
    }
}
