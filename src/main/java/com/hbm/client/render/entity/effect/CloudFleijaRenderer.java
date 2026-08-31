package com.hbm.client.render.entity.effect;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderCloudFleija} (61 lines, read in full) - the
 * companion-cloud renderer for {@link EntityCloudFleija}: one expanding sphere mesh, scaled by
 * {@code entity.age + partialTicks}, textured with {@code BlastFleija.png}, drawn full-bright.
 * CE's own {@code doRender}:
 * <pre>
 * pushMatrix(); translate(x,y,z); disableLighting(); disableLighting(); enableCull();
 * float s = cloud.age + partialTicks; scale(s,s,s);
 * bindTexture(blastTexture); blastModel.renderAll();
 * enableLighting(); enableLighting(); popMatrix();
 * </pre>
 * (the doubled {@code disableLighting()}/{@code enableLighting()} calls are CE's own harmless
 * copy-paste duplication, not ported as two calls - a single disable/no-op-equivalent is all this
 * class needs since 1.21.1 has no fixed-function lighting toggle to begin with, see below).
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li>{@code translate(x,y,z)}/{@code pushMatrix}/{@code popMatrix} - not ported as explicit
 *       calls: {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher#render} already
 *       wraps every {@link #render} call in its own {@code poseStack.pushPose()}/{@code
 *       translate(x,y,z)}/{@code popPose()} before/after invoking this method (confirmed by this
 *       port's own already-committed {@code com.hbm.client.render.ConstantRenderSweep}, which
 *       drives the identical dispatcher call and documents this exact wrapping in its own
 *       javadoc) - so the incoming {@link PoseStack} is already positioned at the entity's world
 *       location when {@link #render} begins, matching every other {@code EntityRenderer} in this
 *       port (see {@code EmptyEntityRenderer}/{@code FallbackEntityRenderer}, neither of which
 *       translates either).</li>
 *   <li>{@code disableLighting()}/{@code enableLighting()} - CE's fixed-function per-vertex normal
 *       lighting toggle has no 1.21.1 equivalent (the modern pipeline is lightmap/shader driven,
 *       not per-vertex Gouraud lighting) - nothing to port, the shader this class's {@link
 *       HbmObjModel#renderType} render type uses never applies directional lighting in the first
 *       place.</li>
 *   <li>{@code enableCull()} - CE explicitly single-sides this draw. This class instead uses
 *       {@link HbmObjModel#renderType(ResourceLocation)}'s established double-sided ({@code
 *       entityCutoutNoCull}) default (see that method's own javadoc and this port's {@code
 *       ArmorModelBase}/{@code HevArmorModel} precedent for the same convention) - a convex sphere
 *       viewed from outside looks identical either way, so this is a deliberate "reuse the
 *       framework's one shared convenience RenderType rather than add a second bespoke one for a
 *       purely-cosmetic difference" call, not a missed detail.</li>
 *   <li>{@code getBrightnessForRender() -> 15728880} (CE, {@code EntityCloudFleija.java}) has no
 *       1.21.1 {@code Entity}-side equivalent to port onto the already-committed entity class.
 *       {@code EntityRenderer<T>} has <b>no {@code getPackedLight(T, float)} method to override</b>
 *       in 1.21.1 - the packed light for a draw is computed upstream by {@code
 *       EntityRenderDispatcher#getPackedLightCoords} and simply handed to {@link #render} as its
 *       {@code packedLight} parameter (confirmed by this port's own {@code
 *       com.hbm.client.render.ConstantRenderSweep}, which calls that exact dispatcher method).
 *       Full-bright is instead achieved the confirmed-real way {@code upstream/neo-edition}'s own
 *       {@code RenderTom.java}/{@code RenderDeathBlast.java} do it (neither overrides any such
 *       method) - by ignoring the incoming {@code packedLight} argument inside {@link #render} and
 *       substituting {@link LightTexture#FULL_BRIGHT} directly at the draw call (packs to the exact
 *       same {@code 0xF000F0}/{@code 15728880} CE's own magic number already encodes).</li>
 * </ul>
 *
 * <h2>Asset gap (flagged, not fixed here - not this task's job)</h2>
 * Neither {@code models/sphere.obj} nor {@code textures/models/explosion/blastfleija.png} exist
 * anywhere in this port's {@code src/main/resources} yet (confirmed by directory search; CE's own
 * real on-disk filenames are lowercase - {@code assets/hbm/models/sphere.obj}/{@code
 * assets/hbm/textures/models/explosion/blastfleija.png} - despite CE's Java source referencing
 * mixed-case {@code "Sphere.obj"}/{@code "BlastFleija.png"}, which only works on CE's original
 * case-insensitive-filesystem dev/ship environment; this class deliberately uses the real lowercase
 * on-disk names since 1.21.1's {@link ResourceLocation} strictly rejects uppercase paths). This
 * class compiles and is wired correctly today; it will render nothing (or throw a resource-missing
 * error on first use - see {@link HbmObjModel#load}) until a future asset-copying pass ports both
 * files in, exactly the same already-flagged gap this report's sibling {@code c1} RBMK-renderer
 * task hit for its own OBJ/texture assets.
 */
public class CloudFleijaRenderer extends EntityRenderer<EntityCloudFleija> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/sphere.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/explosion/blastfleija.png");

    public CloudFleijaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityCloudFleija entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        float s = entity.age + partialTick;

        poseStack.pushPose();
        poseStack.scale(s, s, s);

        VertexConsumer consumer = buffer.getBuffer(HbmObjModel.renderType(TEXTURE));
        // CE: EntityCloudFleija.getBrightnessForRender() -> 15728880 (full-bright) - see class
        // javadoc. EntityRenderer<T> has no getPackedLight(T, float) hook to override in 1.21.1
        // (confirmed by upstream/neo-edition's own RenderTom.java/RenderDeathBlast.java, neither of
        // which override any such method - both substitute LightTexture.FULL_BRIGHT directly at the
        // draw call instead), so the incoming packedLight parameter is deliberately ignored here.
        HbmObjModel.get(MODEL).renderAll(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCloudFleija entity) {
        return TEXTURE;
    }
}
