package com.hbm.client.render.entity.effect;

import com.hbm.client.render.ConstantRenderSweep;
import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Ported from CE's {@code com.hbm.render.entity.effect.RenderCloudTom} (117 lines, read in full) -
 * the "smoke column" renderer for {@link EntityCloudTom}: a procedurally-generated tapered cylinder
 * (16 radial segments x 5 stacked rings, no OBJ mesh - CE builds this shape by hand every frame
 * with raw {@code BufferBuilder} calls, unlike the other 3 renderers in this package which are OBJ
 * meshes) that widens with {@code entity.age + partialTicks} while keeping a fixed absolute height,
 * alpha-fading from opaque at the base to transparent at the top of each ring, with the bound
 * texture continuously vertically scrolling. CE's own {@code doRender} (this task's own brief
 * already flags this as an {@link com.hbm.interfaces.IConstantRenderer} entity - drawn only inside
 * the constant-render sweep, never during vanilla's normal per-chunk entity pass):
 * <pre>
 * if (!ClientProxy.renderingConstant) return;
 * pushMatrix(); translate(x,y,z); disableLighting(); enableBlend(); disableAlpha(); disableCull();
 * shadeModel(SMOOTH); depthMask(false); blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO);
 * double scale = blast.age + partialTicks;
 * int segments = 16; float angle = toRadians(360/segments); int height = 20; int depth = 20;
 * bindTexture(tomblast); [texture-matrix translate by scrolling `movement`]
 * begin(GL_QUADS, POSITION_TEX_COLOR);
 * for (i in 0..segments) for (j in 0..5) {
 *     mod = 1 - j*0.025; h = height + j*10; off = 1D/j;   // NB: off is Infinity at j==0, see below
 *     vec = Vec3d(scale,0,0).rotateYaw(angle*i); x0=vec.x*mod; z0=vec.z*mod;
 *     pos(x0,h,z0).tex(0,1+off).color(1,1,1,0);   pos(x0,-depth,z0).tex(0,0+off).color(1,1,1,1);
 *     vec = vec.rotateYaw(angle); x0=vec.x*mod; z0=vec.z*mod;
 *     pos(x0,-depth,z0).tex(1,0+off).color(1,1,1,1);   pos(x0,h,z0).tex(1,1+off).color(1,1,1,0);
 * }
 * draw(); [restore texture matrix]; depthMask(true); [restore GL state]; popMatrix();
 * </pre>
 *
 * <h2>Preserved CE quirk: division by zero at {@code j == 0}</h2>
 * CE's {@code off = 1D / j} is evaluated with {@code j == 0} on the loop's first iteration - Java
 * {@code double} division by zero does not throw, it yields {@code Double.POSITIVE_INFINITY}
 * (confirmed language semantics, not a guess). This is very likely an unintentional CE bug (the
 * loop variable is used as a divisor starting from 0 rather than 1), but per this port's ground
 * rule 1 ("CE is the sole source of truth for behavior... never assume") it is preserved verbatim
 * here rather than silently "fixed" - {@link #emit} is fed the literal {@code Infinity} v-coordinate
 * CE's own GL_QUADS immediate-mode path would have too. The practical visual impact is expected to
 * be small: the *top* corner of every {@code j==0} quad already has vertex alpha {@code 0} (fully
 * transparent, blended away regardless of its texture coordinate), and only the *bottom* corner of
 * that one ring (alpha {@code 1}, visible) actually samples the texture at an infinite/degenerate V
 * - a real, CE-authentic cosmetic artifact on the innermost ring, not a porting mistake. Flagged
 * explicitly here rather than assumed away, matching this task's own ground rules on unverified/odd
 * source behavior.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code IConstantRenderer} gate</b>: CE's {@code ClientProxy.renderingConstant} static
 *       flag is this port's already-committed {@code com.hbm.client.render.ConstantRenderSweep}
 *       (see that class's own javadoc for the full mechanism) - {@link #render} early-returns
 *       unless {@link ConstantRenderSweep#isRenderingConstant()}, the direct port of CE's own
 *       {@code if (!ClientProxy.renderingConstant) return;} guard.</li>
 *   <li><b>No {@link PoseStack} scale call</b>: unlike the other 3 renderers in this package, CE
 *       never calls {@code GlStateManager.scale} for this entity - the {@code scale} variable is
 *       baked directly into each vertex's computed world-space offset ({@code vec = Vec3d(scale,0,0)
 *       .rotateYaw(...)}) instead. Reproduced identically: this class computes final vertex
 *       positions in Java and only ever calls {@link PoseStack#last()}{@code .pose()} once, to get
 *       the (already entity-translated, per {@link
 *       com.hbm.client.render.entity.effect.CloudFleijaRenderer}'s class javadoc) position matrix
 *       to feed each emitted vertex through.</li>
 *   <li><b>Per-segment outward normal, not CE's implicit fixed-function none</b>: CE's fixed-function
 *       pipeline never computes a lighting normal for raw {@code POSITION_TEX_COLOR} immediate-mode
 *       geometry (irrelevant anyway with {@code disableLighting()} active). The 1.21.1 entity-
 *       translucent shader this class's {@link #SMOKE} render type uses <i>does</i> read the vertex
 *       normal for its own diffuse-light term, so - to avoid an unwanted uniform darkening/lighting
 *       artifact a constant placeholder normal would introduce - each quad's two rotational corners
 *       are given their own true outward-radial unit-vector normal ({@code dir0}/{@code dir1}, the
 *       unscaled rotation direction before the {@code mod}/{@code scale} radius multiply), matching
 *       vanilla's own "each face gets its own reasonable normal" convention (see {@code
 *       HbmObjModel}'s identical flat-face-normal fallback for an unrelated but structurally similar
 *       precedent already established in this port).</li>
 *   <li><b>Texture-matrix scroll → per-vertex V offset</b>: CE scrolls the bound texture via
 *       {@code GlStateManager.matrixMode(GL_TEXTURE); translate(0, movement, 0);} - the fixed-
 *       function texture matrix has no 1.21.1 equivalent (no per-{@code RenderType} texture-matrix
 *       concept exists in the modern pipeline). Reproduced by adding the identical {@code movement}
 *       scalar straight into every emitted vertex's V coordinate instead - algebraically identical
 *       to translating the whole UV space by the same amount before sampling. CE derives {@code
 *       movement} from {@code Minecraft.getMinecraft().player.ticksExisted} (the <i>local player's</i>
 *       own tick counter, not this entity's age - preserved as-is, including the odd choice of
 *       driver) - ported to {@link LocalPlayer#tickCount}, guarded against a null {@code
 *       Minecraft.player} (CE has no such guard; added defensively since this entity's {@link
 *       #render} can in principle run during the brief window before the local player exists,
 *       which CE's own 1.12 client lifecycle does not have an equivalent race for).</li>
 *   <li><b>Custom {@link #SMOKE} render type</b>: textured, standard (non-additive) alpha
 *       translucency, no cull, no depth write, lightmap-affected (CE never overrides this entity's
 *       brightness, unlike its 3 siblings in this package - see {@code EntityCloudTom}'s own
 *       javadoc, which confirms it has no {@code getBrightnessForRender} override). Built the same
 *       confirmed-real way {@code upstream/neo-edition}'s own {@code com.hbm.render.NtmRenderTypes}
 *       (its {@code SMOTH_NO_DEPTH}/{@code NUKE_CLOUDS} fields - same {@code
 *       RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER} shader, {@code TextureStateShard}, {@code
 *       RenderType.NO_CULL}, {@code RenderType.LIGHTMAP}/{@code OVERLAY}, {@code
 *       RenderStateShard.TRANSLUCENT_TARGET} output) builds its own textured-translucent entity
 *       render types at this exact {@code neo_version=21.1.228} - used strictly to confirm this API
 *       shape, not copied for behavior. The blend function itself
 *       ({@link RenderStateShard.TransparencyStateShard} {@link #TRANSLUCENT_ALPHA}) reproduces CE's
 *       exact {@code blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO)} call bit-for-bit,
 *       built the same confirmed-real way {@code NtmRenderTypes.SEVEN_SEVEN_ONE_ZERO} is (a
 *       {@code RenderSystem.blendFuncSeparate(SourceFactor, DestFactor, SourceFactor, DestFactor)}
 *       call inside a bespoke shard's setup lambda).</li>
 *   <li><b>{@code GL_QUADS} mode preserved</b>: {@link VertexFormat.Mode#QUADS} is still a real,
 *       used mode in this exact {@code neo_version} (confirmed by {@code NtmRenderTypes}' own
 *       extensive {@code Mode.QUADS} usage) - no need to fan-triangulate CE's quad loop into
 *       triangles by hand.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * {@code textures/models/explosion/tomblast.png} does not exist in this port's resources yet - see
 * {@link CloudFleijaRenderer}'s class javadoc for the identical, already-flagged gap (real CE
 * on-disk filename is lowercase {@code tomblast.png}, matching CE's own {@code
 * ResourceManager.tomblast} field name and this class's own texture constant below). This entity
 * has no OBJ mesh dependency at all (see class javadoc - the cylinder is generated procedurally).
 */
public class CloudTomRenderer extends EntityRenderer<EntityCloudTom> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/explosion/tomblast.png");

    /** CE: {@code GlStateManager.tryBlendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO)}. */
    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT_ALPHA = new RenderStateShard.TransparencyStateShard(
            "hbm_cloud_tom_translucent",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            },
            () -> {
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }
    );

    private static final RenderType SMOKE = RenderType.create(
            "hbm_cloud_tom_smoke",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1024,
            true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_ALPHA)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .createCompositeState(false)
    );

    private static final int SEGMENTS = 16;
    private static final float ANGLE = (float) Math.toRadians(360D / SEGMENTS);
    private static final int HEIGHT = 20;
    private static final int DEPTH = 20;

    public CloudTomRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityCloudTom entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        // CE: if (!ClientProxy.renderingConstant) return; - see class javadoc.
        if (!ConstantRenderSweep.isRenderingConstant()) return;

        double scale = entity.age + partialTick;

        LocalPlayer player = Minecraft.getInstance().player;
        // CE: `-(Minecraft.getMinecraft().player.ticksExisted + partialTicks) * 0.005F * 10` - see
        // class javadoc for the texture-matrix-to-per-vertex-offset translation and the null guard.
        float movement = player != null ? -(player.tickCount + partialTick) * 0.005F * 10F : 0F;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(SMOKE);

        for (int i = 0; i < SEGMENTS; i++) {
            Vec3 dir0 = new Vec3(1, 0, 0).yRot(ANGLE * i);
            Vec3 dir1 = dir0.yRot(ANGLE);
            double px0 = dir0.x * scale, pz0 = dir0.z * scale;
            double px1 = dir1.x * scale, pz1 = dir1.z * scale;

            for (int j = 0; j < 5; j++) {
                double mod = 1 - j * 0.025;
                double h = HEIGHT + j * 10;
                // CE: `double off = 1D / j;` - Infinity at j==0, preserved verbatim, see class javadoc.
                double off = 1D / j;
                float v0 = (float) off + movement;
                float v1 = (float) (1 + off) + movement;

                double x0 = px0 * mod, z0 = pz0 * mod;
                double x1 = px1 * mod, z1 = pz1 * mod;

                emit(consumer, matrix, x0, h, z0, 0F, v1, 0F, dir0, packedLight);
                emit(consumer, matrix, x0, -DEPTH, z0, 0F, v0, 1F, dir0, packedLight);
                emit(consumer, matrix, x1, -DEPTH, z1, 1F, v0, 1F, dir1, packedLight);
                emit(consumer, matrix, x1, h, z1, 1F, v1, 0F, dir1, packedLight);
            }
        }
    }

    private static void emit(VertexConsumer consumer, Matrix4f matrix, double x, double y, double z,
                              float u, float v, float alpha, Vec3 normal, int packedLight) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor(1F, 1F, 1F, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCloudTom entity) {
        return TEXTURE;
    }
}
