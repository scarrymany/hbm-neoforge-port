package com.hbm.client.render.entity.mob;

import com.hbm.client.render.misc.BeamPronter;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderUFO} (82 lines, {@code extends
 * Render<EntityUFO>}, read in full) - see {@code docs/phase5/boss_and_vehicle_entity_renderers.md}
 * section C, the most mechanically interesting renderer in that report short of the black hole: a
 * continuously Y-spinning OBJ saucer body (spin is a flat function of {@code entity.tickCount}, fully
 * independent of the entity's own facing - CE's real, deliberate behavior, not a bug) plus a
 * conditional 3-layer abduction-beam column via the shared {@link BeamPronter} helper.
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * translate(x, y+1, z);
 * if (!alive) rotate(deathTime+30+partialTicks, 1,0,1);      // death-tumble tilt
 * scale = 2D; bindTexture(ufo_tex);
 * pushMatrix();
 *   rot = (ticksExisted+partialTicks)*5 % 360;
 *   rotate(rot, Y); scale(scale,scale,scale); shadeModel(SMOOTH);
 *   ufo.renderAll();                                          // ufo.obj, single unnamed group
 * popMatrix();
 * if (ufo.getBeam()) {
 *   iy = [downward raycast from posY to the first non-air block, capped at world floor];
 *   length = posY - iy;
 *   if (length &gt; 0) {
 *     prontBeam(Vec3d(0,-length,0), SPIRAL, SOLID, 0x101020,0x101020, 0,              (length+1), 0F,   6, scale*0.75F);
 *     prontBeam(Vec3d(0,-length,0), RANDOM, SOLID, 0x202060,0x202060, ticksExisted/2, (length/2+1), scale*1.5F, 2, 0.0625F);
 *     prontBeam(Vec3d(0,-length,0), RANDOM, SOLID, 0x202060,0x202060, ticksExisted/4, (length/2+1), scale*1.5F, 2, 0.0625F);
 *   }
 * }
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code ufo.getBeam()}</b> - now a real, synced accessor; see {@link EntityUFO#DATA_BEAM}'s
 *       own javadoc for the small, necessary server-side fix this task made alongside this renderer
 *       (the exact gap {@code docs/phase5/boss_and_vehicle_entity_renderers.md} Headline finding #5
 *       flagged).</li>
 *   <li><b>Downward block scan</b> - reproduced client-side exactly as CE does it (CE's own comment
 *       in the boss/vehicle report confirms "client can do this scan itself against its own loaded
 *       chunk data - no new server sync needed for that part"), against {@code
 *       Minecraft.getInstance().level} via the renderer's own captured {@link Level} reference (see
 *       constructor).</li>
 *   <li><b>{@code entity.deathTime}</b> - a public {@code LivingEntity} field, well-established
 *       stable Mojang-mapping knowledge, <b>not independently jar-verified in this sandbox</b>
 *       (matching this port's standing disclosure convention for identical unverified-but-well-known
 *       field accesses elsewhere).</li>
 *   <li><b>{@code entity.tickCount}</b> - CE's {@code ticksExisted}, renamed 1:1 (same rename already
 *       confirmed throughout this task's sibling renderers).</li>
 *   <li><b>Lazy {@link HbmObjModel#get(ResourceLocation)}</b> - same established convention as this
 *       task's other OBJ-based renderers.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mobs/ufo.obj} nor {@code textures/entity/ufo.png} exist in this port's
 * {@code src/main/resources} yet - same already-flagged, already-accepted gap as this task's other
 * OBJ-dependent renderers.
 */
public class UfoRenderer extends EntityRenderer<EntityUFO> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mobs/ufo.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/ufo.png");

    private HbmObjModel cachedModel;

    public UfoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityUFO entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);

        if (!entity.isAlive()) {
            // CE: rotate(deathTime+30+partialTicks, 1,0,1) - GL normalizes the (1,0,1) axis
            // internally; JOML's rotateAxis requires an already-normalized axis, so it is
            // normalized here (1/sqrt(2), 0, 1/sqrt(2)) rather than passed raw.
            float invSqrt2 = (float) (1D / Math.sqrt(2D));
            poseStack.mulPose(new Quaternionf().rotateAxis(
                    (float) Math.toRadians(entity.deathTime + 30 + partialTick),
                    invSqrt2, 0F, invSqrt2));
        }

        double scale = 2D;

        poseStack.pushPose();
        double rot = (entity.tickCount + partialTick) * 5 % 360D;
        poseStack.mulPose(Axis.YP.rotationDegrees((float) rot));
        poseStack.scale((float) scale, (float) scale, (float) scale);

        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        model().renderAll(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        if (entity.getBeam()) {
            Level level = entity.level();
            int ix = (int) Math.floor(entity.getX());
            int iz = (int) Math.floor(entity.getZ());
            int iy = level.getMinBuildHeight();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(ix, 0, iz);

            for (int i = (int) Math.ceil(entity.getY()); i >= level.getMinBuildHeight(); i--) {
                cursor.setY(i);
                if (!level.getBlockState(cursor).isAir()) {
                    iy = i;
                    break;
                }
            }

            double length = entity.getY() - iy;
            if (length > 0) {
                BeamPronter.prontBeam(poseStack, new Vec3(0, -length, 0), BeamPronter.WaveType.SPIRAL,
                        BeamPronter.BeamType.SOLID, 0x101020, 0x101020, 0, (int) (length + 1), 0F, 6,
                        (float) scale * 0.75F);
                BeamPronter.prontBeam(poseStack, new Vec3(0, -length, 0), BeamPronter.WaveType.RANDOM,
                        BeamPronter.BeamType.SOLID, 0x202060, 0x202060, entity.tickCount / 2,
                        (int) (length / 2 + 1), (float) scale * 1.5F, 2, 0.0625F);
                BeamPronter.prontBeam(poseStack, new Vec3(0, -length, 0), BeamPronter.WaveType.RANDOM,
                        BeamPronter.BeamType.SOLID, 0x202060, 0x202060, entity.tickCount / 4,
                        (int) (length / 2 + 1), (float) scale * 1.5F, 2, 0.0625F);
            }
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUFO entity) {
        return TEXTURE;
    }
}
