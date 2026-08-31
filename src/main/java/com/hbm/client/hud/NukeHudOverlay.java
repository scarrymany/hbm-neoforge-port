package com.hbm.client.hud;

import com.hbm.config.ClientConfig;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Ported from CE's nuclear-detonation "camera shake" HUD feature - which, per {@code docs/phase5/
 * reactor_and_explosion_visual_effects.md} Headline finding 6, is not camera shake at all: it is
 * (a) a full-screen white translucent quad drawn over the HUD, and (b) a 2D translate applied only
 * to the hotbar layer, plus (c) - not this class's job, see {@code TorexRenderer} - a fake vanilla
 * hurt-animation trigger that produces the actual "the nuke shook the screen" sensation via
 * vanilla's own damage-vignette/tilt system.
 * <p>
 * CE source (both effects live in {@code com.hbm.main.ModEventHandlerClient.onOverlayRender},
 * {@code RenderGameOverlayEvent.Pre}):
 * <pre>
 * // NUKE FLASH - ElementType.CROSSHAIRS
 * if (event.getType() == CROSSHAIRS &amp;&amp; (flashTimestamp+flashDuration-now) &gt; 0 &amp;&amp; NUKE_HUD_FLASH.get()) {
 *     ... full-screen POSITION_COLOR quad, blendFunc(SRC_ALPHA, ONE), alpha = (flashTimestamp+flashDuration-now)/flashDuration ...
 *     return;
 * }
 * // NUKE GUI SHAKE - ElementType.HOTBAR
 * if (event.getType() == HOTBAR &amp;&amp; (shakeTimestamp+shakeDuration-now) &gt; 0 &amp;&amp; NUKE_HUD_SHAKE.get()) {
 *     double mult = (shakeTimestamp+shakeDuration-now) / (double) shakeDuration * 2;
 *     double horizontal = clamp(sin(now*0.02), -0.7, 0.7) * 15;
 *     double vertical = clamp(sin(now*0.01+2), -0.7, 0.7) * 3;
 *     GlStateManager.translate(horizontal*mult, vertical*mult, 0);
 * }
 * </pre>
 * {@code flashTimestamp}/{@code shakeTimestamp} are written exactly once anywhere in CE - by {@code
 * RenderTorex.doRender} - and read exactly here; this class owns the fields for the identical
 * reason CE's {@code ModEventHandlerClient} (the class doing the reading) owns them rather than
 * {@code RenderTorex} (the class doing the writing) - see {@code TorexRenderer}'s own javadoc for
 * the cross-class write.
 *
 * <h2>1.21.1 API shape</h2>
 * Both {@code RenderGuiLayerEvent.Pre}/{@code RenderGuiEvent.Post} and every rendering primitive
 * below are confirmed real via {@code docs/phase5/hud_overlays_geiger_armor_gun.md}'s own already-
 * verified citation table (that report explicitly uses this exact CE feature as its worked example,
 * per this task's own brief) and independently re-confirmed here by reading {@code upstream/
 * neo-edition/.../main/NuclearTechModClient.java} (lines 257-297) directly, which already ported
 * this exact CE feature against this exact {@code neo_version=21.1.228} - cross-checked strictly for
 * API shape (event types, {@code GuiGraphics}/{@code Tesselator}/{@code RenderSystem} call shapes),
 * never for behavior (see the two deliberate deviations below).
 * <p>
 * <b>Flash: {@link RenderGuiEvent.Post}</b>, matching Neo Edition's real choice, not CE's own
 * {@code ElementType.CROSSHAIRS} {@code Pre}-with-early-{@code return} (CE's early return does not
 * actually cancel the crosshair from drawing - Forge's {@code Pre} firing before an element does
 * not by itself suppress it, and CE never calls {@code event.setCanceled(true)} here either - so
 * CE's real behavior is "draw the flash quad, then still draw the crosshair on top of/after it,"
 * which a whole-frame {@code Post} overlay reproduces identically and more simply).
 * <p>
 * <b>Shake: {@link RenderGuiLayerEvent.Pre} alone</b>, filtered to {@link VanillaGuiLayers#HOTBAR}
 * - matching CE's own {@code ElementType.HOTBAR} gate literally (per this task's own brief: "a
 * GuiGraphics.pose() translate on just the hotbar HUD element"), a deliberately <em>narrower</em>
 * choice than Neo Edition's own real implementation, which applies the identical translate against
 * the whole-frame {@code RenderGuiEvent.Pre} (i.e. shakes every HUD element, not just the hotbar -
 * a real behavioral deviation from CE this port does not reproduce). The translate below is
 * deliberately left un-popped, matching CE's own real structure exactly (CE never calls a matching
 * "un-translate" either): CE's version relies on Forge 1.12's {@code GuiIngameForge} wrapping each
 * {@code ElementType}'s Pre/render/Post trio in its own {@code GlStateManager.pushMatrix()}/{@code
 * popMatrix()} internally (well-established Forge behavior) so the shift never leaks past the
 * hotbar element. Whether NeoForge's own {@code LayeredDraw}/{@code RenderGuiLayerEvent} dispatch
 * does the equivalent {@link GuiGraphics#pose()} push/pop around each named layer is <b>not
 * confirmed in this sandbox</b> (no compiling call site in either reference tree demonstrates a
 * matching {@code RenderGuiLayerEvent.Post}-based pop for this exact scenario, and this port's own
 * ground rules prefer not inventing an unconfirmed nested-class API over a purely cosmetic risk) -
 * flagged explicitly as an open, low-stakes risk: if NeoForge does not auto-isolate layers this way,
 * the shake could visually bleed into whatever HUD element renders immediately after the hotbar for
 * the remainder of that one frame, self-correcting the very next frame regardless (the translate is
 * recomputed, not accumulated, every {@code Pre} call).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class NukeHudOverlay {

    /** CE: {@code ModEventHandlerClient.flashDuration}/{@code shakeDuration} - wall-clock milliseconds, deliberately NOT tick-counted (see class javadoc / research report Headline finding 6: this protects the fade curve from tick-rate hiccups/lag). */
    public static final int FLASH_DURATION = 5_000;
    public static final int SHAKE_DURATION = 1_500;

    /** CE: {@code ModEventHandlerClient.flashTimestamp}/{@code shakeTimestamp} - written exclusively by {@code TorexRenderer.render}, read exclusively here. */
    public static long flashTimestamp;
    public static long shakeTimestamp;

    private NukeHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        long now = System.currentTimeMillis();
        long remaining = flashTimestamp + FLASH_DURATION - now;
        if (remaining <= 0) return;
        if (!ClientConfig.NUKE_HUD_FLASH.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int width = gfx.guiWidth();
        int height = gfx.guiHeight();
        float brightness = remaining / (float) FLASH_DURATION;

        // CE: GlStateManager.disableTexture2D(); enableBlend(); blendFunc(SRC_ALPHA, ONE); depthMask(false);
        // 1.21.1 shape confirmed real via upstream/neo-edition's own NuclearTechModClient.onRenderGuiPost
        // (see class javadoc) - identical Tesselator/BufferBuilder/POSITION_COLOR/BufferUploader idiom
        // already cross-confirmed by docs/phase5/hud_overlays_geiger_armor_gun.md's own citation table.
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(width, 0, 0).setColor(1F, 1F, 1F, brightness);
        buf.addVertex(0, 0, 0).setColor(1F, 1F, 1F, brightness);
        buf.addVertex(0, height, 0).setColor(1F, 1F, 1F, brightness);
        buf.addVertex(width, height, 0).setColor(1F, 1F, 1F, brightness);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    @SubscribeEvent
    public static void onRenderHotbarPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        if (!shouldShake()) return;

        applyShakeTranslate(event.getGuiGraphics());
    }

    private static boolean shouldShake() {
        if (!ClientConfig.NUKE_HUD_SHAKE.get()) return false;
        long now = System.currentTimeMillis();
        return (shakeTimestamp + SHAKE_DURATION - now) > 0;
    }

    private static void applyShakeTranslate(GuiGraphics gfx) {
        long now = System.currentTimeMillis();
        double mult = (shakeTimestamp + SHAKE_DURATION - now) / (double) SHAKE_DURATION * 2;
        double horizontal = Mth.clamp(Math.sin(now * 0.02), -0.7, 0.7) * 15;
        double vertical = Mth.clamp(Math.sin(now * 0.01 + 2), -0.7, 0.7) * 3;
        gfx.pose().translate(horizontal * mult, vertical * mult, 0);
    }
}
