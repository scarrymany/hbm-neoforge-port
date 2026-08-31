package com.hbm.client.render;

import com.hbm.interfaces.IConstantRenderer;
import com.hbm.main.MainRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 1.21.1 replacement for CE's {@code ClientProxy.renderingConstant} / {@code
 * ModEventHandlerClient.renderWorld} "constant render" sweep - the mechanism {@code docs/phase5/
 * reactor_and_explosion_visual_effects.md}'s Headline finding 5 fully researched and resolved
 * (read there in full for the "why": several VFX entities' visual footprint - a mushroom cloud, a
 * black hole - can legitimately exceed any single chunk's normal render-distance/visibility rules,
 * so CE deliberately force-renders them a second time every frame, bypassing vanilla's per-entity
 * frustum cull entirely, rather than trying to keep them inside it).
 *
 * <p><b>CE's exact mechanism</b> ({@code upstream/hbm-ce/.../main/ModEventHandlerClient.java:421-446},
 * {@code renderWorld}, a {@code RenderWorldLastEvent} handler at {@code EventPriority.LOWEST} so it
 * runs after vanilla's own normal per-chunk entity render pass): sets a static
 * {@code ClientProxy.renderingConstant = true} flag, iterates <em>every</em> entity in {@code
 * mc.world.loadedEntityList} (not just the ones vanilla's frustum test already decided to draw),
 * and for each one implementing {@link IConstantRenderer} force-calls {@code Render.doRender(e,
 * relX, relY, relZ, yaw, partialTicks)} a second time with hand-interpolated,
 * camera-relative position/yaw - then resets the flag to {@code false}. Every CE {@code
 * IConstantRenderer} renderer early-returns ({@code if (!ClientProxy.renderingConstant) return;})
 * during vanilla's own normal pass and only actually draws inside this explicit second sweep; this
 * is the load-bearing reason those renderers also set {@code ignoreFrustumCheck = true} /
 * override {@code isInRangeToRenderDist} to always return {@code true} on CE's own entity side -
 * they intentionally never rely on vanilla's culling to decide "should I draw" at all.
 *
 * <p><b>This class is purely the driver/sweep half of that mechanism.</b> {@link
 * IConstantRenderer} itself (a 7-line marker interface) and its 5 current implementors -
 * {@code EntityTom}, {@code EntityCloudTom}, {@code EntityNukeTorex}, {@code EntityOrbitalLaser},
 * {@code EntityDeathBlast} (grep-confirmed exhaustive as of this class's own addition) - are
 * already real, already committed, and are <b>not</b> touched here. None of them currently guard
 * their render body with an {@code isRenderingConstant()} check (their real Content-wave renderers
 * do not exist yet either - they are all still registered to {@link EmptyEntityRenderer} in
 * {@link ClientEntityRenderers}), so this sweep force-invoking their normal render path a second
 * time today is inert (draws nothing extra) but load-bearing for the future: once a real
 * Content-wave renderer for any of these 5 entities lands and adds CE's own early-return guard
 * (the obvious, faithful port of {@code if (!ClientProxy.renderingConstant) return;}, spelled
 * {@code if (!ConstantRenderSweep.isRenderingConstant()) return;} against this class), that
 * renderer will only ever actually draw because this sweep exists and calls it a second time -
 * exactly the dependency Headline finding 5 names.
 *
 * <h2>1.21.1 API-shape decisions</h2>
 * <ul>
 *   <li><b>Hook: {@code net.neoforged.neoforge.client.event.RenderLevelStageEvent}, on
 *   {@link RenderLevelStageEvent.Stage#AFTER_ENTITIES}</b> - a distinct {@code Stage} from the
 *   sibling {@code com.hbm.particle.engine.EngineHandler}'s own {@code Stage.AFTER_WEATHER}
 *   particle-draw hook, per this task's brief and the research report's own Key design decision
 *   ("a distinct {@code Stage} from {@code particle_engine_and_generic_vfx.md}'s own {@code
 *   AFTER_WEATHER} particle-draw stage... e.g. {@code AFTER_ENTITIES}, still to be picked at
 *   implementation time"). {@code AFTER_ENTITIES} was confirmed to actually exist as a real
 *   {@code RenderLevelStageEvent.Stage} enum constant via web search against NeoForge's own hosted
 *   1.21.x javadoc (this sandbox has no NeoForge jar to grep directly, so this web search is the
 *   verification, not a jar-level confirmation): it fires immediately after vanilla's normal
 *   opaque-block-and-entity pass and before translucent blocks/
 *   tripwire/particles/weather, i.e. still well before {@code AFTER_WEATHER} in the same frame -
 *   exactly the "runs once per frame, after/alongside the normal entity pass" slot CE's own {@code
 *   RenderWorldLastEvent} (which in 1.12 fires once, after everything, with no sub-stages) occupied.
 *   Running before {@code AFTER_WEATHER} rather than after does not matter for this sweep's own
 *   correctness (it is a fully separate draw call per entity, order-independent relative to the
 *   particle engine's own batch), but is worth documenting since it inverts the two systems' firing
 *   order relative to how CE's single-stage 1.12 event models them as simultaneous.
 *   <li><b>Event bus: game bus ({@code NeoForge.EVENT_BUS}), no {@code bus = Bus.MOD} override</b>
 *   - confirmed two independent ways, not assumed: (1) {@code EngineHandler}'s own already-committed,
 *   already-reviewed javadoc already established (citing Neo Edition's real, compiling {@code
 *   EngineHandler}) that {@code RenderLevelStageEvent} is an ordinary game-bus event, not an {@code
 *   IModBusEvent}; (2) independently re-confirmed here via web search against NeoForge's own docs,
 *   which state plainly that {@code RenderLevelStageEvent}'s sub-events "are fired on the main
 *   NeoForge event bus, only on the logical client" - i.e. {@code NeoForge.EVENT_BUS} (the game bus),
 *   never the mod bus. Ground rule 5's "mod-bus events need an explicit {@code bus=MOD}" warning
 *   does not apply to this class for the same reason it does not apply to {@code EngineHandler}.
 *   <li><b>{@code EntityRenderDispatcher.render(E, double x, double y, double z, float yRot, float
 *   partialTick, PoseStack, MultiBufferSource, int packedLight)}</b> (camera-relative {@code x,y,z},
 *   matching CE's own {@code relX = d0 - d3} camera-relative pattern 1:1) and {@code
 *   EntityRenderDispatcher.getPackedLightCoords(E, float)} - both well-established Minecraft
 *   rendering API confirmed via web search against hosted 1.21.x javadoc mirrors (exact parameter
 *   list matched), <b>not verified against a real compiled jar in this sandbox</b> (network policy
 *   blocks the actual Maven/Mojang artifact hosts); flagged per this project's standing "unverified"
 *   convention rather than silently assumed.
 *   <li><b>{@code ClientLevel.entitiesForRendering()}</b> (returns {@code Iterable<Entity>}, the
 *   exact pool {@code LevelRenderer} itself iterates before any per-entity frustum decision is
 *   made) is the 1.21.1 equivalent of CE's {@code mc.world.loadedEntityList} - both are "every
 *   entity the client currently tracks in a loaded area," not a frustum-filtered subset. Confirmed
 *   via web search (consistent across 1.18.2 through 1.21 per NeoForge/Forge javadoc mirrors);
 *   not verified against a real jar.
 *   <li>Buffer source: reuses {@code Minecraft.getInstance().renderBuffers().bufferSource()} +
 *   trailing {@code buffer.endBatch()}, the exact already-committed, already-reviewed pattern
 *   {@code EngineHandler.onRenderLevelStage} already established in this same codebase for the
 *   sibling particle-draw stage - reused rather than re-derived.
 * </ul>
 *
 * <p><b>Deliberately not ported</b>: CE's per-entity camera-relative delta is computed against
 * {@code mc.getRenderViewEntity()}'s own hand-interpolated position ({@code d3/d4/d5} in CE's
 * source); this class instead uses {@link RenderLevelStageEvent#getCamera()}'s already-interpolated
 * {@link Camera#getPosition()} - the same camera-space origin {@code LevelRenderer} itself passes
 * into every normal entity's {@code render} call this same frame (confirmed by the {@code
 * EntityRenderDispatcher.render} signature above taking camera-relative doubles, meaning some
 * upstream caller - {@code LevelRenderer}, in vanilla's own normal pass - always subtracts a camera
 * origin before calling it; {@link RenderLevelStageEvent#getCamera()} is that same origin exposed to
 * event subscribers). This is the more direct/correct 1.21.1 equivalent of CE's "render-view entity
 * position," not a behavioral change - a spectator camera detached from any entity has no "render
 * view entity" to subtract in CE's own model either, whereas the {@code Camera} object here is
 * always well-defined.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class ConstantRenderSweep {

    private ConstantRenderSweep() {}

    /**
     * Port of CE's {@code ClientProxy.renderingConstant} static flag. {@code true} only for the
     * duration of {@link #onRenderLevelStage}'s sweep loop below - a future Content-wave {@code
     * IConstantRenderer} entity's real renderer should early-return unless this is {@code true},
     * exactly matching CE's own {@code if (!ClientProxy.renderingConstant) return;} guard on every
     * one of its {@code IConstantRenderer} renderers. Deliberately not placed on {@code
     * com.hbm.main.ClientProxy} itself: that file is a shared aggregator this task's ground rules
     * forbid editing directly (see this class's own task's ground rule 7) - this port's {@code
     * ClientProxy} also does not otherwise mirror CE's like-named rendering-registration proxy at
     * all (it only carries the keybind-proxy slice), so re-homing this one flag onto the class that
     * actually drives it is the correct call, not a workaround.
     */
    private static boolean renderingConstant = false;

    /**
     * Whether the current call stack is inside this class's constant-render sweep. A future
     * {@code IConstantRenderer} entity's {@code EntityRenderer#render} override should check this
     * and early-return {@code false} otherwise, mirroring CE's own {@code
     * if (!ClientProxy.renderingConstant) return;} guard so the entity draws only during the sweep,
     * never during vanilla's own normal (frustum-culled) entity pass.
     */
    public static boolean isRenderingConstant() {
        return renderingConstant;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        // RenderLevelStageEvent#getPartialTick() returns net.minecraft.client.DeltaTracker, not a
        // plain float (confirmed against the real 1.21.1 NeoForge source,
        // github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/client/
        // event/RenderLevelStageEvent.java) - EntityRenderDispatcher#render/#getPackedLightCoords
        // below still take a plain float, so it must be extracted explicitly. Same
        // DeltaTracker#getGameTimeDeltaPartialTick(boolean) call already used, and confirmed against
        // upstream/neo-edition's own real, compiling equivalent, by this port's own
        // com.hbm.particle.engine.ParticleEngineNT#render (see EngineHandler.onRenderLevelStage,
        // which passes the DeltaTracker straight through to that method instead of unwrapping it here).
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        renderingConstant = true;
        try {
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof IConstantRenderer)) continue;

                // CE: d0/d1/d2 = lastTickPos + (pos - lastTickPos) * partialTicks, then relX = d0 - d3
                // (the render-view entity's own identically-interpolated position). Here d3/d4/d5's
                // 1.21.1 equivalent is the already-interpolated camera position above - see class
                // javadoc's "Deliberately not ported" note for why that substitution is correct.
                double relX = Mth.lerp(partialTick, entity.xo, entity.getX()) - camPos.x;
                double relY = Mth.lerp(partialTick, entity.yo, entity.getY()) - camPos.y;
                double relZ = Mth.lerp(partialTick, entity.zo, entity.getZ()) - camPos.z;
                float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

                int packedLight = dispatcher.getPackedLightCoords(entity, partialTick);
                dispatcher.render(entity, relX, relY, relZ, yaw, partialTick, event.getPoseStack(), buffer, packedLight);
            }
        } finally {
            // Mirrors CE's own try-less-but-still-unconditional reset (ClientProxy.renderingConstant
            // = false; runs unconditionally after CE's loop too) - wrapped in finally here purely so
            // a future IConstantRenderer renderer throwing mid-sweep can never wedge the flag stuck
            // true forever (CE has no such protection and does not need it: a Forge 1.12 render
            // exception is typically caught and logged per-entity further up the call stack in a way
            // this class cannot rely on here without its own try/finally).
            renderingConstant = false;
        }
        buffer.endBatch();
    }
}
