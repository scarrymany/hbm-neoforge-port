package com.hbm.client.render.entity.mob;

import com.hbm.main.MainRegistry;

import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderQuacc} (53 lines, {@code extends
 * RenderChicken}, read in full - CE has no separate duck-specific renderer, {@code RenderQuacc} is
 * shared by both {@code EntityDuck} and {@code EntityQuackos}; see {@link QuackosRenderer}'s own
 * javadoc for why this port splits it into two classes anyway) - see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md} section F. Duck itself is CE's unscaled base case: only the
 * texture is overridden, no {@code preRenderCallback} scale.
 *
 * <p>This exact "thin {@code ChickenRenderer} subclass, texture override only" shape is confirmed
 * real and compiling at this exact {@code neo_version} by {@code upstream/neo-edition}'s own {@code
 * com.hbm.render.entity.mob.DuckRenderer} (21 lines, read in full, cross-checked strictly for API
 * shape per this port's ground rules) - this class is a close structural match, differing only in
 * package/texture-constant naming to match this port's own conventions.
 */
public class DuckRenderer extends ChickenRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/duck.png");

    public DuckRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken chicken) {
        return TEXTURE;
    }
}
