package com.hbm.client.render.entity.effect;

import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderCloudSolinium} (65 lines, read in full) - the
 * companion-cloud renderer for {@link EntityCloudSolinium}: one opaque cyan sphere plus 3
 * progressively-larger, additively-blended "glow shell" copies of the same sphere layered on top,
 * all textureless (pure vertex-color tint), drawn full-bright. CE's own {@code doRender}:
 * <pre>
 * pushMatrix(); translate(x,y,z); disableLighting(); disableCull(); shadeModel(SMOOTH); disableTexture2D();
 * int color = 0x27FFDA;
 * float scale = entity.age + partialTicks; scale(scale,scale,scale);
 * color(fr,fg,fb);                       // opaque, alpha defaults to 1
 * sphere_new.renderAll();                // inner opaque sphere - no blending yet
 * enableBlend(); blendFunc(SRC_ALPHA, ONE);
 * color(fr,fg,fb, 0.125F);
 * enableCull();
 * double outerScale = 1.025;
 * for (i in 0..3) { scale(outerScale,outerScale,outerScale); sphere_new.renderAll(); } // compounding scale, 3 additive layers
 * color(1,1,1,1); disableBlend(); enableLighting(); enableTexture2D(); shadeModel(FLAT); popMatrix();
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li>See {@link CloudFleijaRenderer}'s class javadoc for the shared "no explicit translate/
 *       pushMatrix, no fixed-function lighting toggle, {@code getPackedLight} is the correct
 *       full-bright hook" reasoning - identical here, not re-derived.</li>
 *   <li><b>Textureless colored sphere - two bespoke {@link RenderType}s, not {@link
 *       HbmObjModel#renderType}.</b> CE never binds a texture for this entity at all ({@code
 *       disableTexture2D()}, no {@code bindTexture} call anywhere in its {@code doRender}) - a pure
 *       vertex-color draw. {@link HbmObjModel#renderType} always assumes a bound texture ({@code
 *       RenderType.entityCutoutNoCull(texture)}), so this class instead builds {@link #SOLID}/
 *       {@link #GLOW}, two textureless {@code DefaultVertexFormat.POSITION_COLOR_LIGHTMAP} render
 *       types - the exact format/shader pairing ({@code RenderType.RENDERTYPE_TEXT_BACKGROUND_SHADER}
 *       paired with {@code POSITION_COLOR_LIGHTMAP}, {@code RenderType.NO_TEXTURE}) confirmed real
 *       and compiling at this exact {@code neo_version=21.1.228} by {@code upstream/neo-edition}'s
 *       own {@code com.hbm.particle.AmatFlashParticle} ({@code AMAT} render type field) - used
 *       strictly to confirm this API shape, not copied for behavior (that particle's own visual
 *       logic is unrelated).</li>
 *   <li><b>{@link #SOLID}</b> mirrors CE's inner-sphere GL state: {@code disableCull()} (→ {@link
 *       RenderStateShard#NO_CULL}) and no blending (→ {@link RenderStateShard#NO_TRANSPARENCY},
 *       vanilla's real constant that calls {@code RenderSystem.disableBlend()} - confirmed to exist
 *       and to do exactly that by {@code upstream/neo-edition}'s own {@code
 *       com.hbm.render.NtmRenderTypes}, whose code comment ({@code "// original NO_TRANSPARENCY
 *       disabled blending... why mojang???"}) explicitly documents having deliberately avoided the
 *       real constant elsewhere in that file for an unrelated reason - i.e. independently confirms
 *       both that the real constant exists and exactly what it does).</li>
 *   <li><b>{@link #GLOW}</b> mirrors CE's 3 outer layers: {@code enableCull()} (→ {@link
 *       RenderStateShard#CULL} - the enabled-culling counterpart to {@code NO_CULL}; well-established
 *       Minecraft rendering API, not directly demonstrated by a compiling call site in either
 *       reference tree read this session, flagged per this port's standing convention) and CE's
 *       exact {@code blendFunc(SRC_ALPHA, ONE)} additive blend, reproduced bit-for-bit via a bespoke
 *       {@link RenderStateShard.TransparencyStateShard} ({@link #ADDITIVE_BLEND}) built the same
 *       confirmed-real way {@code NtmRenderTypes}' own identically-named/-shaped {@code
 *       ADDITIVE_BLEND} field is built (same {@code RenderSystem.enableBlend()}/{@code
 *       blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE)}/{@code defaultBlendFunc()}/{@code
 *       disableBlend()} call sequence) - chosen over the built-in {@code
 *       RenderStateShard.LIGHTNING_TRANSPARENCY} (which is additive-*adjacent* but not
 *       confirmed bit-identical to CE's plain 2-argument {@code blendFunc} call) specifically so
 *       this glow's blend math matches CE exactly, not approximately - ground rule 1 (CE is the
 *       sole source of truth for numbers/behavior) applied literally.</li>
 *   <li><b>Compounding outer scale</b>: CE's {@code GlStateManager.scale} calls inside the loop are
 *       cumulative GL-matrix-stack multiplications (each iteration multiplies the *already-scaled*
 *       matrix again) - reproduced here identically via repeated {@code poseStack.scale(...)} calls
 *       inside the same loop, which compound the same way on {@link PoseStack}'s own matrix
 *       stack.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * {@code models/sphere.obj} does not exist in this port's resources yet - see {@link
 * CloudFleijaRenderer}'s class javadoc for the identical, already-flagged gap (this class shares
 * the same sphere mesh CE's own {@code ResourceManager.sphere_new} does, matching CE's own reuse of
 * one sphere asset across both {@code RenderCloudFleija}/{@code RenderCloudSolinium}). Unlike the
 * other 3 renderers in this package, Solinium needs no texture asset at all (CE never binds one).
 */
public class CloudSoliniumRenderer extends EntityRenderer<EntityCloudSolinium> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/sphere.obj");

    /** CE: {@code int color = 0x27FFDA;} - the cyan-teal tint for both the inner sphere and the outer glow. */
    private static final int TINT_R_G_B = 0x27FFDA;
    private static final float TINT_R = ((TINT_R_G_B >> 16) & 0xFF) / 255F;
    private static final float TINT_G = ((TINT_R_G_B >> 8) & 0xFF) / 255F;
    private static final float TINT_B = (TINT_R_G_B & 0xFF) / 255F;

    /** CE: {@code GlStateManager.blendFunc(SRC_ALPHA, ONE)} - see class javadoc for why this is bespoke, not {@code LIGHTNING_TRANSPARENCY}. */
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_BLEND = new RenderStateShard.TransparencyStateShard(
            "hbm_cloud_solinium_additive",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }
    );

    /** Inner opaque sphere pass - CE: {@code disableCull()}, no blending (drawn before {@code enableBlend()}). */
    private static final RenderType SOLID = RenderType.create(
            "hbm_cloud_solinium_solid",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.TRIANGLES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TEXT_BACKGROUND_SHADER)
                    .setTextureState(RenderType.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    /** Outer 3 additive glow-shell passes - CE: {@code enableCull()} + {@code blendFunc(SRC_ALPHA, ONE)}. */
    private static final RenderType GLOW = RenderType.create(
            "hbm_cloud_solinium_glow",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.TRIANGLES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TEXT_BACKGROUND_SHADER)
                    .setTextureState(RenderType.NO_TEXTURE)
                    .setTransparencyState(ADDITIVE_BLEND)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .setCullState(RenderStateShard.CULL)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .createCompositeState(false)
    );

    public CloudSoliniumRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityCloudSolinium entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        float scale = entity.age + partialTick;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        HbmObjModel model = HbmObjModel.get(MODEL);

        // Inner opaque sphere - alpha 1 (CE: GlStateManager.color(fr,fg,fb), 3-arg = alpha defaults to 1).
        VertexConsumer solidConsumer = buffer.getBuffer(SOLID);
        model.renderAll(poseStack, solidConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                packArgb(TINT_R, TINT_G, TINT_B, 1F));

        // 3 outer additive glow shells - alpha 0.125, compounding 1.025x scale each iteration
        // (CE: `double outerScale = 1.025; for (i<3) { scale(outerScale,...); sphere.renderAll(); }`).
        VertexConsumer glowConsumer = buffer.getBuffer(GLOW);
        int glowArgb = packArgb(TINT_R, TINT_G, TINT_B, 0.125F);
        for (int i = 0; i < 3; i++) {
            poseStack.scale(1.025F, 1.025F, 1.025F);
            model.renderAll(poseStack, glowConsumer, packedLight, OverlayTexture.NO_OVERLAY, glowArgb);
        }

        poseStack.popPose();
    }

    /** CE: {@code EntityCloudSolinium.getBrightnessForRender() -> 15728880} - see {@link CloudFleijaRenderer} javadoc. */
    @Override
    public int getPackedLight(EntityCloudSolinium entity, float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    /** CE binds no texture for this entity at all - see class javadoc. Never called by this class's own render path. */
    @Override
    public ResourceLocation getTextureLocation(EntityCloudSolinium entity) {
        return null;
    }

    private static int packArgb(float r, float g, float b, float a) {
        return (clampByte(a) << 24) | (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(b);
    }

    private static int clampByte(float c) {
        return Math.round(Math.max(0F, Math.min(1F, c)) * 255F);
    }
}
