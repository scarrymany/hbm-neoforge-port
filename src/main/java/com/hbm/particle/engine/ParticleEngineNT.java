package com.hbm.particle.engine;

import com.hbm.config.GeneralConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Singleton batch owner/renderer for {@link ParticleNT}, replacing CE's raw-GL
 * {@code InstancedParticleRenderer} (177 lines, a shared {@code ArrayDeque} capped at 16384, batched
 * by {@code ParticleInstanced.RenderType} into one {@code glDrawArraysInstanced} call per type) with
 * vanilla's own already-hardware-batched {@link net.minecraft.client.renderer.RenderType}/
 * {@link com.mojang.blaze3d.vertex.VertexConsumer} upload path instead - see {@link ParticleNT}'s
 * class javadoc for why no raw-GL/shader replacement is needed at all
 * ({@code docs/phase5/particle_engine_and_generic_vfx.md} Finding 1).
 * <p>
 * Structurally adapted from {@code upstream/neo-edition/.../particle/engine/ParticleEngineNT.java}
 * (86 lines, read in full) - confirmed real/compiling API shape only (never behavior/content), per
 * this project's standing rule.
 * <p>
 * <b>{@code INSTANCED_PARTICLES} redefined as a density knob, not a capability gate</b>
 * ({@code particle_engine_and_generic_vfx.md}'s Finding 2 / "Recommended architecture" point 3, and
 * directly answering {@link GeneralConfig}'s own class javadoc open question): CE's original GL-3.3
 * capability probe ({@code GLCompat}) has nothing to probe for any more, since neither modern
 * replacement path is raw GL - so the config boolean is repurposed here as a soft cap on this list's
 * size (halved when {@code false}), matching CE's own {@code 16384}-entry {@code ArrayDeque} ceiling
 * when {@code true}. Oldest-first eviction on overflow mirrors CE's own {@code ArrayDeque} behavior
 * (its comment: "if we exceed the max amount of particles, remove the oldest ones").
 * <p>
 * Public API: {@link #add(ParticleNT)} (queues a particle to start ticking/rendering next frame -
 * callable from any client thread state, including mid-tick), {@link #clear()} (called by
 * {@link EngineHandler} on world disconnect). {@link #tick()}/{@link #render} are called once per
 * client tick / once per frame by {@link EngineHandler} - not meant to be called directly by other
 * renderer code.
 */
@OnlyIn(Dist.CLIENT)
public class ParticleEngineNT {

    public static final ParticleEngineNT INSTANCE = new ParticleEngineNT();

    /** CE's own {@code InstancedParticleRenderer.MAX_PARTICLES} ceiling, applied when {@link GeneralConfig#INSTANCED_PARTICLES} is on. */
    private static final int MAX_PARTICLES = 16384;

    private final List<ParticleNT> pendingParticles = new ArrayList<>();
    private final List<ParticleNT> particles = new ArrayList<>();

    private ParticleEngineNT() {
    }

    /** Queues a particle to be ticked/rendered starting next tick. Safe to call from any client-side code, not just inside a tick. */
    public void add(ParticleNT effect) {
        this.pendingParticles.add(effect);

        int cap = GeneralConfig.INSTANCED_PARTICLES.get() ? MAX_PARTICLES : MAX_PARTICLES / 2;
        while (this.particles.size() + this.pendingParticles.size() > cap && !this.particles.isEmpty()) {
            this.particles.remove(0);
        }
    }

    /** Drops every live and pending particle - called by {@link EngineHandler} on world disconnect so a fresh world starts with an empty batch. */
    public void clear() {
        this.pendingParticles.clear();
        this.particles.clear();
    }

    public void render(BufferSource buffer, Camera camera, DeltaTracker deltaTracker) {
        if (this.particles.isEmpty()) return;

        Map<RenderType, List<ParticleNT>> byRenderType = new HashMap<>();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        for (ParticleNT particle : this.particles) {
            if (particle == null || particle.dead) continue;
            RenderType type = particle.getRenderType();
            byRenderType.computeIfAbsent(type, t -> new ArrayList<>()).add(particle);
        }

        for (Map.Entry<RenderType, List<ParticleNT>> entry : byRenderType.entrySet()) {
            RenderType type = entry.getKey();
            VertexConsumer consumer = type != null ? buffer.getBuffer(type) : null;
            for (ParticleNT particle : entry.getValue()) {
                particle.render(consumer, camera, partialTick);
            }
        }
    }

    public void tick() {
        if (this.particles.isEmpty() && this.pendingParticles.isEmpty()) return;

        if (!this.pendingParticles.isEmpty()) {
            this.particles.addAll(this.pendingParticles);
            this.pendingParticles.clear();
        }

        Iterator<ParticleNT> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            ParticleNT particle = iterator.next();
            particle.tick();
            if (particle.dead) {
                iterator.remove();
            }
        }

        if (!this.pendingParticles.isEmpty()) {
            this.particles.addAll(this.pendingParticles);
            this.pendingParticles.clear();
        }
    }

    /** Current live (non-pending) particle count - diagnostic/debug use. */
    public int size() {
        return this.particles.size();
    }
}
