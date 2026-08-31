package com.hbm.client.render.entity.mob;

import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderQuacc} (53 lines, {@code extends
 * RenderChicken}, read in full - the same class CE reuses for both {@code EntityDuck} and {@code
 * EntityQuackos}) - see {@code docs/phase5/boss_and_vehicle_entity_renderers.md} section F.
 * {@code EntityQuackos} needs its own separate registered {@code EntityRenderer} from {@link
 * DuckRenderer} even though both share this same renderer-class shape (CE reuses one class, this
 * port uses two thin sibling classes) - two distinct {@code EntityType}s each need their own {@link
 * net.minecraft.client.renderer.entity.EntityRenderers#register} call regardless, and giving Quackos
 * its own class is the cleanest way to add its one extra {@code preRenderCallback}-equivalent
 * override without an {@code instanceof} branch inside a shared class.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code preRenderCallback(Entity, float) -&gt; scale(Chicken, PoseStack, float)}</b> - CE's
 *       {@code GlStateManager.scale(25,25,25)} override, matching {@link EntityQuackos}'s own
 *       25x-scaled hitbox per {@code docs/phase4/entities_bosses.md}. The modern hook point/method
 *       name/signature ({@code scale(T livingEntity, PoseStack poseStack, float partialTickTime)})
 *       is confirmed real and compiling at this exact {@code neo_version} by {@code
 *       upstream/neo-edition}'s own {@code com.hbm.render.entity.mob.CreeperNuclearRenderer}'s
 *       identical {@code scale(Creeper, PoseStack, float)} override (37 lines, read in full,
 *       cross-checked strictly for API shape, not behavior) - {@code Chicken} substituted 1:1 for
 *       {@code Creeper} here, the exact same override shape applied to a different {@code
 *       LivingEntityRenderer} subclass.</li>
 * </ul>
 */
public class QuackosRenderer extends ChickenRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/duck.png");

    public QuackosRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Chicken livingEntity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(25F, 25F, 25F);
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken chicken) {
        return TEXTURE;
    }
}
