package com.hbm.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ParticleRenderType} factory for this task's (Phase 5 {@code c14-custom-particle-content})
 * particle classes - a texture-bound analogue of CE's own "every particle class binds its own PNG
 * directly" convention (confirmed by reading every CE class under
 * {@code upstream/hbm-ce/.../particle/}: each hand-calls {@code Minecraft.getMinecraft()
 * .getTextureManager().bindTexture(...)} at the top of its own {@code renderParticle}, never a
 * shared atlas), ported onto 1.21.1's {@link ParticleRenderType#begin} contract instead of CE's raw
 * {@code GlStateManager} calls.
 * <p>
 * <b>Why not {@code registerSpriteSet}/{@link net.minecraft.client.particle.TextureSheetParticle}
 * with a real {@link net.minecraft.client.renderer.texture.TextureAtlasSprite}</b>: that path needs
 * the texture stitched into {@link net.minecraft.client.renderer.texture.TextureAtlas#LOCATION_PARTICLES}
 * via a matching {@code assets/hbm/particles/<name>.json}, and {@code com.hbm.client.particle.
 * ModParticleProviders}'s own class javadoc (the {@code f4-particle-registry-and-events} task) already
 * documents zero of those exist yet and deliberately registers every entry via {@code registerSpecial}
 * instead of {@code registerSpriteSet} for exactly this reason - see that class's javadoc for the full
 * reasoning. A {@code registerSpecial} provider owns its own texture binding, so a custom
 * {@link ParticleRenderType} that binds a fixed {@link ResourceLocation} (CE's real per-class texture
 * path, e.g. {@code hbm:textures/particle/shockwave.png}) is the direct, faithful 1.21.1 analogue of
 * CE's own per-class bind call - not a workaround.
 * <p>
 * <b>Interface shape - confirmed compiling, not independently verified against a NeoForge/Minecraft
 * sources jar</b> (this sandbox cannot run {@code ./gradlew} or launch a client - ground rule 3):
 * cross-checked against {@code upstream/neo-edition/.../particle/ModParticleRenderTypes.NONE} (used
 * strictly for the real, compiling method signature - {@code begin(Tesselator, TextureManager)}
 * returning {@link BufferBuilder}, with no {@code end(Tesselator)} override present - meaning that
 * method is either defaulted or no longer part of the per-instance contract on this version's
 * {@link ParticleRenderType}; this class therefore also does not override it, matching that same
 * confirmed-compiling override set exactly). The {@link RenderSystem} shader/texture/blend calls
 * inside {@link #begin} mirror vanilla's own well-established
 * {@code ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT}/{@code PARTICLE_SHEET_LIT} implementations
 * (stable modding knowledge across the 1.17-1.21 line), substituting each entry's own fixed texture
 * for the shared particle atlas - deliberately does NOT touch {@code depthMask} (unlike CE's raw
 * {@code GlStateManager.depthMask(false)} calls) since this type's {@code end()} is not overridden
 * here and an un-paired {@code depthMask(false)} with no matching restore would leak GL state into
 * later draws - a documented, deliberate fidelity trade-off, not an oversight.
 */
public final class HbmParticleRenderTypes {

    private HbmParticleRenderTypes() {
    }

    private static final Map<ResourceLocation, ParticleRenderType> TRANSLUCENT_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, ParticleRenderType> ADDITIVE_CACHE = new HashMap<>();

    /** Standard alpha-blended quad on a fixed texture - CE's common case for smoke/haze/fog-style particles. */
    public static ParticleRenderType translucent(ResourceLocation texture) {
        return TRANSLUCENT_CACHE.computeIfAbsent(texture, tex -> new ParticleRenderType() {
            @Override
            public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                RenderSystem.setShaderTexture(0, tex);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public String toString() {
                return "hbm:translucent[" + tex + "]";
            }
        });
    }

    /**
     * Additive ({@code SRC_ALPHA, ONE}) blended quad on a fixed texture - CE's own
     * {@code GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE)} pattern, used by every
     * CE particle class this task ports that reads as a glow/flash/spark rather than a smoke puff
     * (e.g. {@code ParticleSpark}, {@code ParticleHadron}, {@code ParticlePlasmaBlast},
     * {@code ParticleMukeWave}, {@code ParticleMukeFlash}).
     */
    public static ParticleRenderType additive(ResourceLocation texture) {
        return ADDITIVE_CACHE.computeIfAbsent(texture, tex -> new ParticleRenderType() {
            @Override
            public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                RenderSystem.setShaderTexture(0, tex);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public String toString() {
                return "hbm:additive[" + tex + "]";
            }
        });
    }
}
