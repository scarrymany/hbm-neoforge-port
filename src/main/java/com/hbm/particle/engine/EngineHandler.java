package com.hbm.particle.engine;

import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Client-only event glue that drives {@link ParticleEngineNT}: ticks it once per client tick, draws
 * it once per frame, and clears it on world disconnect. This is the piece
 * {@code docs/phase5/particle_engine_and_generic_vfx.md}'s "Recommended architecture" point 3 and
 * {@code docs/phase5/reactor_and_explosion_visual_effects.md} (a sibling report, headline finding 5)
 * both name by this exact class name as the hook other Phase 5 renderer work - {@code c3}'s mushroom
 * cloud named explicitly - should register alongside for "runs once per frame, after/alongside the
 * normal entity pass" needs.
 * <p>
 * Structurally adapted from {@code upstream/neo-edition/.../particle/engine/EngineHandler.java} (35
 * lines, read in full) - confirmed real/compiling API shape only, per this project's standing rule
 * that Neo Edition is never a source of behavior/content.
 * <p>
 * <b>{@code bus = Bus.MOD} is deliberately NOT set here - confirmed correct via two independent real
 * sources, not assumed from a blanket rule.</b> Ground rule 5 warns every mod-bus event
 * ({@code FMLClientSetupEvent}, {@code RegisterXEvent}, etc.) needs an explicit
 * {@code bus = EventBusSubscriber.Bus.MOD}, but none of the three events this class subscribes to are
 * mod-bus events - all three are ordinary per-frame/per-tick/per-connection <em>game</em>-bus events,
 * confirmed two ways: (1) Neo Edition's own real, compiling {@code EngineHandler} annotates
 * {@code @EventBusSubscriber(value = Dist.CLIENT)} with no {@code bus=} override at all for the exact
 * same three event types ({@code RenderLevelStageEvent}, {@code ClientTickEvent.Pre},
 * {@code ClientPlayerNetworkEvent.LoggingOut}); (2) independently, this port's own already-committed
 * {@code com.hbm.handler.HbmKeybindInputEvents} already subscribes to {@code ClientTickEvent.Post}
 * (same event family) with the identical no-{@code bus=}-override annotation shape and its own
 * javadoc explicitly documents why: {@code InputEvent}/{@code ClientTickEvent} are "game-bus-only".
 * {@code RenderLevelStageEvent} and {@code ClientPlayerNetworkEvent} are likewise ordinary
 * {@code NeoForge.EVENT_BUS} (game-bus) events, not {@code IModBusEvent} - so the default
 * {@code Bus.GAME} this annotation falls back to is the <em>correct</em> choice here, not an
 * oversight. Leaving {@code bus=} unset is therefore deliberate, not the ground-rule-5 mistake it
 * would be for a mod-bus event.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public class EngineHandler {

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ParticleEngineNT.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), event.getPartialTick());
        buffer.endBatch();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (!Minecraft.getInstance().isPaused()) {
            ParticleEngineNT.INSTANCE.tick();
        }
    }
}
