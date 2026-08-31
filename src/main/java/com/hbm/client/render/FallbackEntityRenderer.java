package com.hbm.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Minimal-but-not-invisible {@link EntityRenderer} fallback for entities that are meant to be
 * player-visible, persistent world objects (mobs, bosses, vehicles, trains, crates, carts) but do not
 * yet have a bespoke Content-wave renderer/model ported from CE.
 *
 * <p>Deliberately does <b>not</b> override {@link #render}: the vanilla {@link EntityRenderer#render}
 * default body only draws the entity's nameplate when {@link #shouldShowName} is true (custom name /
 * looked-at, standard vanilla behavior) and draws nothing else — no cube, no box, no bespoke geometry.
 * That is exactly the "draws nothing bespoke" behavior this task's brief asks for: still no crash, but
 * a player standing next to an unrendered UFO or minecart at least sees a nameplate/hitbox marker they
 * can debug against instead of true, silent nothing (harder to distinguish "not spawned" from "spawned
 * but invisible" with {@link EmptyEntityRenderer}).
 *
 * <p>This is a foundation-wave placeholder only — see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md} for the real, per-entity CE-faithful renderer work
 * (OBJ-model rigs, vanilla-mob-model reskins, custom minecart rigs, etc) that is expected to replace
 * each individual {@link net.minecraft.client.renderer.entity.EntityRenderers#register} call this
 * class currently backs, one line at a time, in {@code com.hbm.client.render.ClientEntityRenderers}.
 * CE (1.12.2) never needed an equivalent class: {@code RenderingRegistry
 * .registerEntityRenderingHandler} in {@code upstream/hbm-ce/.../main/ClientProxy.java} was only ever
 * called for entities that already had a real renderer written; skipping it for an entity just meant
 * "draws nothing," with no crash — the exact leniency NeoForge 1.21.1 removes (see
 * {@link EmptyEntityRenderer}'s javadoc for the full explanation of why any registration is now
 * mandatory).
 *
 * <p>{@link #getTextureLocation} returning {@code null} mirrors {@link EmptyEntityRenderer} and is
 * safe for the same reason: nothing in this class's inherited {@code render} path invokes it.
 */
public class FallbackEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    public FallbackEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
