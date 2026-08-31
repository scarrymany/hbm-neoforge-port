package com.hbm.client.render.blockentity.rbmk;

import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodReaSimBlockEntity;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Fuel-column renderer - the {@code hasRod}-gated stack of "Rods" OBJ parts plus the Cherenkov-glow
 * overlay, ported from CE's {@code RenderRBMKLid} ({@code upstream/hbm-ce/.../render/tileentity/
 * RenderRBMKLid.java}, 73 lines, read in full). Backs both {@link RBMKRodBlockEntity} and its
 * {@link RBMKRodReaSimBlockEntity} subclass (CE's plain {@code @AutoRegister} on
 * {@code RenderRBMKLid} - no explicit {@code tileentity=} override - covers
 * {@code TileEntityRBMKRodReaSim} the same way, since that class {@code extends
 * TileEntityRBMKRod} with no rendering-relevant field/behavior override; this port's own
 * {@code BlockEntityType}s are separate registry objects per concrete class, so registration needs
 * two explicit {@code BlockEntityRenderers.register} calls - see this task's own
 * {@code wiringSnippets}).
 *
 * <h2>Headline finding 4 - the field this class must read</h2>
 * CE's {@code TileEntityRBMKRod.fluxQuantity} is a server-only transient accumulator, zeroed every
 * tick after being copied into {@code lastFluxQuantity}; CE's own {@code serialize}/
 * {@code deserialize} pair deliberately mails {@code lastFluxQuantity} across the wire but
 * relabels it as {@code fluxQuantity} on arrival, so CE's own {@code RenderRBMKLid} reading
 * {@code te.fluxQuantity > 5} is really testing "did last tick's completed flux exceed 5." This
 * port's {@link RBMKRodBlockEntity#deserialize} keeps the two fields honestly separate on the wire
 * instead (writes/reads {@code lastFluxQuantity} labeled as {@code lastFluxQuantity}) - so this
 * class reads {@link RBMKRodBlockEntity#lastFluxQuantity}, <b>not</b> {@code .fluxQuantity} (which
 * stays permanently 0 on a client-side instance, since it is never sent). Reading the wrong field
 * would silently never render the glow - see
 * {@code docs/phase5/reactor_and_explosion_visual_effects.md} Headline finding 4 for the full
 * derivation.
 *
 * <h2>Assets not yet present</h2>
 * {@code models/rbmk/rbmk_element_rods.obj} (CE: {@code ResourceManager.rbmk_element_rods_vbo})
 * and {@code textures/block/rbmk/rbmk_element_fuel.png} do not exist in
 * {@code src/main/resources} yet - see this task's own notes. This renderer activates the moment
 * they land, same lazy-{@link HbmObjModel#get} pattern as {@link RBMKControlRodRenderer}.
 */
public final class RBMKFuelColumnRenderer implements BlockEntityRenderer<RBMKRodBlockEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/rbmk/rbmk_element_rods.obj");
    private static final ResourceLocation TEX_FUEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/block/rbmk/rbmk_element_fuel.png");

    // CE: RenderRBMKLid.render's renderCherenkovEffect(0.4F, 0.9F, 1.0F, 0.1F, offset) call - the
    // Cherenkov-blue additive glow tint, unchanged.
    private static final float GLOW_R = 0.4F;
    private static final float GLOW_G = 0.9F;
    private static final float GLOW_B = 1.0F;
    private static final float GLOW_A = 0.1F;

    private HbmObjModel cachedModel;

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(RBMKRodBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!be.hasRod) return;

        // See RBMKControlRodRenderer's own javadoc for why passing null here is the documented-safe
        // call shape from a client-only renderer (RBMKDials' Level parameter is never dereferenced).
        int offset = RBMKDials.getColumnHeight(null);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        // CE: ColorUtil.fr/fg/fb(te.rodColor) - a plain 0xRRGGBB unpack, inlined here rather than
        // depending on a new shared com.hbm.util.ColorUtil this task does not own.
        int rodColor = be.rodColor;
        float r = ((rodColor >> 16) & 0xFF) / 255F;
        float g = ((rodColor >> 8) & 0xFF) / 255F;
        float b = (rodColor & 0xFF) / 255F;

        VertexConsumer fuelConsumer = bufferSource.getBuffer(HbmObjModel.renderType(TEX_FUEL));
        poseStack.pushPose();
        // CE: `for (int i = 0; i <= offset; i++)` - offset + 1 copies, one per column-height unit
        // including the core block's own layer.
        for (int i = 0; i <= offset; i++) {
            model().renderPart(poseStack, fuelConsumer, packedLight, packedOverlay, r, g, b, 1.0F, "Rods");
            poseStack.translate(0.0, 1.0, 0.0);
        }
        poseStack.popPose();

        // See class javadoc's "Headline finding 4" section for why this reads lastFluxQuantity, not
        // fluxQuantity.
        if (be.lastFluxQuantity > 5) {
            renderCherenkovEffect(poseStack, offset);
        }

        poseStack.popPose();
    }

    /** CE: {@code TileEntityRBMKBase.getMaxRenderDistanceSquared() -> 65536.0D} (256 blocks) - see {@link RBMKControlRodRenderer#getViewDistance()}'s own javadoc for the confirmed 1.21.1 shape. */
    @Override
    public int getViewDistance() {
        return 256;
    }

    /**
     * CE: {@code RenderRBMKLid.renderCherenkovEffect} - a stack of translucent, additive-blended
     * horizontal quads spanning the column height, one every 0.25 blocks starting 0.75 blocks above
     * the column base. Drawn as a manual immediate-mode {@link Tesselator}/{@link BufferBuilder}/
     * {@link BufferUploader#drawWithShader} sequence (matching CE's own raw-GL approach) rather than
     * a {@link MultiBufferSource}-batched {@code RenderType}, since additive blending has no
     * standard vanilla {@code RenderType} to reuse and a hand-built {@code RenderType.create(...)}
     * {@code CompositeState} is unnecessary extra unverified API-shape risk for one simple effect.
     * This exact "manual RenderSystem/Tesselator/BufferUploader.drawWithShader" technique is
     * confirmed real by {@code docs/phase5/particle_engine_and_generic_vfx.md} (citing
     * {@code upstream/neo-edition}'s {@code RadiationFogParticle}), and the additive
     * {@code blendFunc(SRC_ALPHA, ONE)} pair is separately confirmed real and used live in that same
     * reference's {@code render/entity/effect/RenderBlackHole.java:181} (cross-checked for API shape
     * only, per this port's ground rules - never for behavior).
     */
    private static void renderCherenkovEffect(PoseStack poseStack, int height) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.75, 0.0);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();

        Matrix4f mat = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // CE: `for (float j = 0.0F; j <= height; j += 0.25F)` - preserved verbatim, including its
        // float-accumulation step (this loop always runs at least once since height >= 1, RBMKDials'
        // configured column height range is [1;15]).
        for (float j = 0.0F; j <= height; j += 0.25F) {
            buffer.addVertex(mat, -0.5F, j, -0.5F).setColor(GLOW_R, GLOW_G, GLOW_B, GLOW_A);
            buffer.addVertex(mat, -0.5F, j, 0.5F).setColor(GLOW_R, GLOW_G, GLOW_B, GLOW_A);
            buffer.addVertex(mat, 0.5F, j, 0.5F).setColor(GLOW_R, GLOW_G, GLOW_B, GLOW_A);
            buffer.addVertex(mat, 0.5F, j, -0.5F).setColor(GLOW_R, GLOW_G, GLOW_B, GLOW_A);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    public static final class Provider implements BlockEntityRendererProvider<RBMKRodBlockEntity> {
        @Override
        public BlockEntityRenderer<RBMKRodBlockEntity> create(BlockEntityRendererProvider.Context context) {
            return new RBMKFuelColumnRenderer();
        }
    }
}
