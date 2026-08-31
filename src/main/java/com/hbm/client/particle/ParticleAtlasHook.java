package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/**
 * Phase 6 whole-tree sweep fix: split out of {@link ModParticleProviders}.
 * <p>
 * {@code docs/phase5/custom_particle_types_registry.md} ("Risk: {@code @EventBusSubscriber} bus
 * default" section, lines 151-157) already confirmed {@link TextureAtlasStitchedEvent} is a real
 * NeoForge <b>client/game-bus</b> event, not {@code IModBusEvent} - explicitly the opposite of
 * {@link net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent} (almost certainly
 * mod-bus, same category as {@code RegisterMenuScreensEvent}). {@link ModParticleProviders}
 * correctly sets {@code bus = EventBusSubscriber.Bus.MOD} for {@code registerProviders} - but that
 * same class-level annotation applies to every {@code @SubscribeEvent} method in the class, so its
 * original {@code onTextureAtlasStitched(TextureAtlasStitchedEvent)} handler (a game-bus event)
 * was silently unreachable, exactly the same "one class, one bus" gotcha the missing-{@code
 * bus=MOD} pattern is the mirror image of - the ground rules' registered footgun, just the other
 * direction: a class narrowed to {@code Bus.MOD} that also carries a game-bus handler.
 * <p>
 * The report's own recommendation was to add both handlers onto {@code ClientModRegistry.java}
 * (already {@code bus = Bus.MOD}) - not workable for a {@code Bus.MOD}-only class hosting a
 * game-bus event either, and {@code ClientModRegistry.java} is a shared aggregator file this task
 * does not edit directly. This tiny standalone class (the same "separate {@code
 * @EventBusSubscriber} class to avoid touching a shared file" pattern already used repeatedly
 * elsewhere in this port, e.g. {@code PowerGenClientRegistry}/{@code TurretClientRegistry}) is the
 * lowest-risk fix: default {@code bus()} (Bus.GAME) is exactly correct here, no override needed.
 * <p>
 * Impact today is zero either way - {@link #particleAtlas} has no reader anywhere in this port yet
 * (no {@code registerSpriteSet}-registered particle type exists, per {@link ModParticleProviders}'s
 * own javadoc), so this is prophylactic: the field will actually get populated once real particle
 * textures/JSON land and a future entry switches to {@code registerSpriteSet}.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class ParticleAtlasHook {

    private ParticleAtlasHook() {
    }

    /** See {@link ModParticleProviders}'s former copy of this same javadoc for the full rationale. */
    public static TextureAtlas particleAtlas;

    @SubscribeEvent
    public static void onTextureAtlasStitched(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) {
            particleAtlas = event.getAtlas();
        }
    }
}
