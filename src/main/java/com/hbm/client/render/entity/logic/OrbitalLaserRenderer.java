package com.hbm.client.render.entity.logic;

import com.hbm.client.render.ConstantRenderSweep;
import com.hbm.entity.logic.EntityOrbitalLaser;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderOrbitalLaser} (86 lines, {@code extends
 * Render<EntityOrbitalLaser>}, read in full) - see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md}'s named {@code IConstantRenderer} list and {@code
 * docs/phase5/reactor_and_explosion_visual_effects.md}'s Headline finding 5. Structurally the
 * simplest of this task's three procedural-fan renderers: the same 8-wedge vertical "light pillar"
 * fan as {@link DeathBlastRenderer} (red outer, white inner - not magenta), with no separate orb
 * effect at all (this entity has no equivalent of {@code renderOrb}/{@code sphere.renderAll()} in
 * CE's real source - confirmed by direct read).
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * if (!renderingConstant) return;
 * disableLighting(); disableTexture2D(); shadeModel(SMOOTH); depthMask(false); enableBlend();
 * blendFunc(SRC_ALPHA, ONE);
 * vector = (0.5,0,0);
 * for i in 0..8: quad(vector*1, y:0-&gt;250) color(1,0,0,1); vector.rotateYaw(45);   // red outer fan
 * for i in 0..8: quad(vector*0.5, y:0-&gt;250) color(1,1,1,1); vector.rotateYaw(45); // white inner fan
 * [restore state]
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * Identical technique to {@link DeathBlastRenderer} - see that class's own javadoc for the full
 * {@code IConstantRenderer}/{@code Vec3.yRot}/raw-{@code Tesselator} translation notes, not repeated
 * here. {@link EntityOrbitalLaser} already {@code implements IConstantRenderer} (a real,
 * already-committed Phase 4 fact, independently re-confirmed by direct read here).
 */
public class OrbitalLaserRenderer extends EntityRenderer<EntityOrbitalLaser> {

    public OrbitalLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(EntityOrbitalLaser entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        if (!ConstantRenderSweep.isRenderingConstant()) return;

        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        emitFan(buffer, matrix, 1.0F, 1F, 0F, 0F, 1F);
        emitFan(buffer, matrix, 0.5F, 1F, 1F, 1F, 1F);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /** Same "recomputed {@code Vec3.yRot} per wedge" idiom as {@link DeathBlastRenderer#emitFan}. */
    private static void emitFan(BufferBuilder buffer, Matrix4f matrix, float radius, float r, float g, float b, float a) {
        for (int i = 0; i < 8; i++) {
            Vec3 v0 = new Vec3(radius, 0, 0).yRot((float) Math.toRadians(45 * i));
            Vec3 v1 = new Vec3(radius, 0, 0).yRot((float) Math.toRadians(45 * (i + 1)));

            buffer.addVertex(matrix, (float) v0.x, 250F, (float) v0.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) v0.x, 0F, (float) v0.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) v1.x, 0F, (float) v1.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) v1.x, 250F, (float) v1.z).setColor(r, g, b, a);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOrbitalLaser entity) {
        return null;
    }
}
