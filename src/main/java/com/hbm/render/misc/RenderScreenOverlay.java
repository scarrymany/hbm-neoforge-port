package com.hbm.render.misc;

import com.hbm.config.ClientConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;

/**
 * Ported from CE's {@code com.hbm.render.misc.RenderScreenOverlay} (532 lines) - see
 * {@code docs/phase5/hud_overlays_geiger_armor_gun.md} (read in full before editing) for this
 * area's complete research. Only the three methods this task's scope actually needs are ported
 * here ({@link #renderRadCounter}/{@link #renderDigCounter} for the Geiger/digamma persistent HUD
 * bars, {@link #renderCustomCrosshairs} for the gun crosshair dispatch); CE's
 * {@code renderAmmo}/{@code renderAmmoAlt}/{@code renderStingerLockon}/{@code renderDashBar}/
 * {@code renderShieldBar}/{@code renderScope}/{@code renderBadges} are real CE methods this report
 * scoped into sibling/deferred work (shield bar, dash bar, badges row, jetpack HUD, legacy-gun ammo
 * counter) - not ported here, see this task's own structured-output notes for exactly what remains.
 * <p>
 * <b>1.21.1 API swap</b> (every primitive below cross-confirmed against this port's own already-
 * committed {@code IItemHUD}/{@code IHoldableWeapon} interfaces and Neo Edition's compiling
 * {@code com.hbm.render.util.RenderScreenOverlay}, at the exact same {@code neo_version=21.1.228}
 * this port targets - see the research report's "Confirmed 1.21.1 API surface" table):
 * {@code ScaledResolution} → {@code Minecraft.getInstance().getWindow()} (
 * {@link Window#getGuiScaledWidth()}/{@link Window#getGuiScaledHeight()}); {@code Gui
 * .drawTexturedModalRect(x, y, u, v, w, h)} → {@link GuiGraphics#blit(ResourceLocation, int, int,
 * int, int, int, int)} (same 256x256-atlas-assumed pixel-space UV convention, confirmed by Neo
 * Edition reusing CE's own {@code overlay_misc.png}/{@code overlay_digamma.png} unmodified - both
 * confirmed still 256x256 PNGs after copying into this port's {@code src/main/resources}); {@code
 * FontRenderer.drawString} → {@link GuiGraphics#drawString(Font, String, int, int, int)}; {@code
 * GlStateManager.pushMatrix/translate/popMatrix} → {@code guiGraphics.pose().pushPose()/...
 * /popPose()} (unused here - {@link #renderCustomCrosshairs} only needs a blend-function change, no
 * transform); {@code GlStateManager.tryBlendFuncSeparate} → {@link RenderSystem#blendFuncSeparate}.
 * <p>
 * The pixel positions/thresholds/colors in {@link #renderRadCounter}/{@link #renderDigCounter} are
 * copied verbatim from CE's real numbers (not Neo Edition's own simplified rewrite, which drops
 * {@code ClientConfig}/{@code RadiationConfig} position offsets and collapses all three RAD/s text
 * colors to a single red) - CE is this port's sole source of truth for behavior/numbers per this
 * wave's ground rules; Neo Edition was cross-checked only for the surrounding API shape
 * (hideGui/spectator/first-person guards, which CE's original relies on the outer
 * {@code RenderGameOverlayEvent} dispatch for and which this port's chosen dispatch strategy -
 * {@code RenderGuiEvent.Post}, a whole-frame event that fires even while F1 is held - needs done
 * explicitly here instead; see {@code com.hbm.render.hud.GeigerHudOverlay}'s own javadoc for why
 * that dispatch strategy was chosen over the per-layer alternative).
 */
public final class RenderScreenOverlay {

    private static final ResourceLocation MISC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/overlay_misc.png");
    private static final ResourceLocation DIGAMMA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/overlay_digamma.png");

    private RenderScreenOverlay() {
    }

    private static long lastRadSurvey;
    private static float prevRadResult;
    private static float lastRadResult;

    private static long lastDigSurvey;
    private static float prevDigResult;
    private static float lastDigResult;

    /**
     * CE: {@code RenderScreenOverlay.renderRadCounter} (47-103). Reads the player's own accumulated
     * total-body dose pool ({@code HbmLivingAttachment#getRads()}) - <b>not</b> the same quantity as
     * the Geiger click-sound/Dosimeter readout ({@code ContaminationUtil#getActualPlayerRads}/
     * {@code HbmLivingAttachment#getRadBuf()}) - see the research report's Key risk #1. Turns the
     * accumulator into an approximate RAD/s delta by sampling once per 1000ms, exactly as CE does.
     */
    public static void renderRadCounter(GuiGraphics guiGraphics, float in) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.options.hideGui) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        float radiation = lastRadResult - prevRadResult;

        if (System.currentTimeMillis() >= lastRadSurvey + 1000) {
            lastRadSurvey = System.currentTimeMillis();
            prevRadResult = lastRadResult;
            lastRadResult = in;
        }

        int length = 74;
        int maxRad = 1000;
        int bar = getScaled(in, maxRad, 74);

        Window window = mc.getWindow();
        int posX = 16 + ClientConfig.GEIGER_OFFSET_HORIZONTAL.get();
        int posY = window.getGuiScaledHeight() - 20 - ClientConfig.GEIGER_OFFSET_VERTICAL.get();

        guiGraphics.blit(MISC_TEXTURE, posX, posY, 0, 0, 94, 18);
        guiGraphics.blit(MISC_TEXTURE, posX + 1, posY + 1, 1, 19, bar, 16);

        if (radiation >= 25) {
            guiGraphics.blit(MISC_TEXTURE, posX + length + 2 + 18, posY, 36, 36, 18, 18);
        } else if (radiation >= 10) {
            guiGraphics.blit(MISC_TEXTURE, posX + length + 2 + 18, posY, 18, 36, 18, 18);
        } else if (radiation >= 2.5) {
            guiGraphics.blit(MISC_TEXTURE, posX + length + 2 + 18, posY, 0, 36, 18, 18);
        }

        Font font = mc.font;
        if (radiation > 1000) {
            guiGraphics.drawString(font, ">1000 RAD/s", posX, posY - 8, 0xFF0000);
        } else if (radiation >= 1) {
            guiGraphics.drawString(font, Math.round(radiation) + " RAD/s", posX, posY - 8, 0xFFFF00);
        } else if (radiation > 0) {
            guiGraphics.drawString(font, "<1 RAD/s", posX, posY - 8, 0x00FF00);
        }
    }

    /**
     * CE: {@code RenderScreenOverlay.renderDigCounter} (106-162). Same rate-sampling shape as
     * {@link #renderRadCounter}, over the separate digamma accumulator
     * ({@code HbmLivingAttachment#getDigamma()}), positioned by {@link RadiationConfig#DIGAMMA_X}/
     * {@link RadiationConfig#DIGAMMA_Y} (already-ported config, matching CE's {@code digammaX}/
     * {@code digammaY} 1:1).
     */
    public static void renderDigCounter(GuiGraphics guiGraphics, float in) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.options.hideGui) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        float digamma = lastDigResult - prevDigResult;

        if (System.currentTimeMillis() >= lastDigSurvey + 1000) {
            lastDigSurvey = System.currentTimeMillis();
            prevDigResult = lastDigResult;
            lastDigResult = in;
        }

        int length = 74;
        int maxDig = 10;
        int bar = getScaled(in, maxDig, 74);

        Window window = mc.getWindow();
        int posX = RadiationConfig.DIGAMMA_X.get();
        int posY = window.getGuiScaledHeight() - 18 - RadiationConfig.DIGAMMA_Y.get();

        guiGraphics.blit(DIGAMMA_TEXTURE, posX, posY, 0, 218, 94, 18);
        guiGraphics.blit(DIGAMMA_TEXTURE, posX + 1, posY + 1, 1, 237, bar, 16);

        if (digamma >= 0.25) {
            guiGraphics.blit(DIGAMMA_TEXTURE, posX + length + 2 + 18, posY, 108, 72, 18, 18);
        } else if (digamma >= 0.1) {
            guiGraphics.blit(DIGAMMA_TEXTURE, posX + length + 2 + 18, posY, 90, 72, 18, 18);
        } else if (digamma >= 0.025) {
            guiGraphics.blit(DIGAMMA_TEXTURE, posX + length + 2 + 18, posY, 72, 72, 18, 18);
        }

        Font font = mc.font;
        if (digamma > 0.1) {
            guiGraphics.drawString(font, ">100 mDRX/s", posX, posY - 8, 0xCC0000);
        } else if (digamma >= 0.01) {
            guiGraphics.drawString(font, ((int) Math.round(digamma * 1000D)) + " mDRX/s", posX, posY - 8, 0xFF0000);
        } else if (digamma > 0) {
            guiGraphics.drawString(font, "<10 mDRX/s", posX, posY - 8, 0xFF3232);
        }
    }

    private static int getScaled(double cur, double max, double scale) {
        return (int) Math.min(cur / max * scale, scale);
    }

    /**
     * CE: {@code RenderScreenOverlay.renderCustomCrosshairs} (170-187). Dispatched from
     * {@code ItemGunBaseNT.renderHUD} (Sedna guns) and {@code com.hbm.render.hud.ItemHudDispatcher}
     * ({@link com.hbm.interfaces.IHoldableWeapon} items, e.g. {@code ItemLaserDetonator}) whenever
     * the vanilla {@code minecraft:crosshair} layer fires and the held item wants a custom one.
     * Ported 1:1 from Neo Edition's own confirmed-real, currently-running port of this exact method
     * ({@code com.hbm.render.util.RenderScreenOverlay#renderCustomCrosshairs}, 74-90) - the "inverted
     * colors" blend function ({@code ONE_MINUS_DST_COLOR}/{@code ONE_MINUS_SRC_COLOR}) is CE's own
     * real crosshair blend mode, not a Neo Edition invention.
     */
    public static void renderCustomCrosshairs(GuiGraphics guiGraphics, Crosshair cross) {
        if (cross == Crosshair.NONE) return;

        Window window = Minecraft.getInstance().getWindow();
        int size = cross.size;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        guiGraphics.blit(MISC_TEXTURE, window.getGuiScaledWidth() / 2 - (size / 2), window.getGuiScaledHeight() / 2 - (size / 2),
                cross.x, cross.y, size, size);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Verbatim port of CE's {@code RenderScreenOverlay.Crosshair} enum (texture atlas x/y/size only). */
    public enum Crosshair {
        NONE(0, 0, 0),
        CROSS(1, 55, 16),
        CIRCLE(19, 55, 16),
        SEMI(37, 55, 16),
        KRUCK(55, 55, 16),
        DUAL(1, 73, 16),
        SPLIT(19, 73, 16),
        CLASSIC(37, 73, 16),
        BOX(55, 73, 16),

        L_CROSS(0, 90, 32),
        L_KRUCK(32, 90, 32),
        L_CLASSIC(64, 90, 32),
        L_CIRCLE(96, 90, 32),
        L_SPLIT(0, 122, 32),
        L_ARROWS(32, 122, 32),
        L_BOX(64, 122, 32),
        L_CIRCUMFLEX(96, 122, 32),
        L_RAD(0, 154, 32),
        L_MODERN(32, 154, 32),
        L_BOX_OUTLINE(64, 154, 32);

        public final int x;
        public final int y;
        public final int size;

        Crosshair(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
}
