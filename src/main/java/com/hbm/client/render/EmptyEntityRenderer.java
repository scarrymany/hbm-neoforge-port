package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Fully invisible {@link EntityRenderer} for entities that are pure server-side logic/collision
 * objects with no meaningful body of their own (projectiles, blast/cloud FX markers, dummy seat
 * or bounding-box entities, gravity wells, etc).
 *
 * <p>CE (1.12.2 Forge) never needed this: {@code net.minecraftforge.fml.client.registry
 * .RenderingRegistry.registerEntityRenderingHandler(Class, IRenderFactory)} was optional per-entity
 * — an unregistered entity simply never drew anything and did not crash. NeoForge 1.21.1 removed that
 * leniency: every {@code EntityType} reaching {@code EntityRenderDispatcher} without a renderer
 * registered via {@link net.minecraft.client.renderer.entity.EntityRenderers#register} throws. This
 * class is the deliberate, explicit "yes, still nothing to draw" analogue of skipping registration in
 * 1.12.2, cross-checked against {@code upstream/neo-edition}'s own identically-shaped
 * {@code com.hbm.render.entity.EmptyEntityRenderer} (confirmed real, compiling NeoForge 1.21.1 code —
 * used strictly to confirm this exact API shape is legal, not copied verbatim: this file adds the
 * javadoc above and lives in this port's own {@code com.hbm.client.render} package instead of
 * {@code com.hbm.render.entity}).
 *
 * <p>{@link #getTextureLocation} returning {@code null} is safe here: the base
 * {@link EntityRenderer#render} override below never calls it, and nothing else in the vanilla render
 * dispatch path calls {@code getTextureLocation} for an {@code EntityRenderer} that isn't a
 * {@code LivingEntityRenderer}/similar subclass that binds a skin texture itself.
 */
public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    public EmptyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        // Intentionally empty - no nameplate, no body. See class javadoc.
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
