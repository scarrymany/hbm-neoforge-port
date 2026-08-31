package com.hbm.client.render.blockentity.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Autoloader piston animation, ported from CE's {@code RenderRBMKAutoloader}
 * ({@code upstream/hbm-ce/.../render/tileentity/RenderRBMKAutoloader.java}, 62 lines, read in full
 * - only the {@code render(...)} half, not the {@code IItemRendererProvider}/in-hand item-render
 * half, which is {@code com.hbm.client.render.item}'s territory, not this task's). Translates the
 * shared "Piston" OBJ part by the live extraction fraction, exactly like
 * {@link RBMKControlRodRenderer}'s rod-lid translate - the simplest of this task's 4 renderers.
 *
 * <h2>CE field name note</h2>
 * CE's own {@code render()} actually reads {@code press.renderPiston} (a third, client-only-
 * smoothed field {@code TileEntityRBMKAutoloader} maintains alongside {@code piston}/
 * {@code lastPiston} - see that class's {@code update()}'s client branch: {@code renderPiston}
 * eases toward a synced {@code syncPiston} snapshot over {@code turnProgress} ticks). This port's
 * {@link RBMKAutoloaderBlockEntity} collapses that three-field scheme down to the plain
 * {@code piston}/{@code lastPiston} pair this task's own instructions name
 * ({@code Mth.lerp(partialTick, lastPiston, piston)}), matching that block entity's own javadoc
 * ("simplified piston-animation timing; core swap logic preserved"). This renderer reads exactly
 * the two fields the task asked for.
 *
 * <h2>Known gap this renderer does not fix (out of this task's file scope)</h2>
 * {@link RBMKAutoloaderBlockEntity#updateEntity()} only advances {@code lastPiston = piston} inside
 * its server-side branch (it returns immediately on {@code level.isClientSide}), unlike the sibling
 * {@code RBMKControlBlockEntity#updateEntity()} which explicitly copies
 * {@code lastExtraction = extraction} every tick on <b>both</b> sides for exactly this kind of
 * partial-tick interpolation. On a real client, {@code lastPiston} therefore never advances past
 * whatever it was before the block entity's own class was constructed there (its default {@code 0.0}
 * field value) - {@code Mth.lerp} below will interpolate correctly the moment the block entity
 * package gains the equivalent client-side copy, but until then this renderer's animation will look
 * like a repeated snap-from-0 rather than a smooth glide between synced network updates. This is a
 * data-field gap in {@code RBMKAutoloaderBlockEntity} (owned by the Phase 2/4 RBMK block-entity
 * package, not this rendering task per this wave's own file-ownership ground rule) - flagged here
 * and in this task's own notes for the coordinator to route back to that package's owner, not fixed
 * in this file.
 *
 * <h2>Assets not yet present</h2>
 * {@code models/rbmk/autoloader.obj} (CE: {@code ResourceManager.rbmk_autoloader}) and
 * {@code textures/models/machines/rbmk_autoloader.png} (CE: {@code ResourceManager.rbmk_autoloader_tex})
 * do not exist in {@code src/main/resources} yet. Same lazy-{@link HbmObjModel#get} activation
 * pattern as this task's other 3 renderers.
 */
public final class RBMKAutoloaderPistonRenderer implements BlockEntityRenderer<RBMKAutoloaderBlockEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/rbmk/autoloader.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/machines/rbmk_autoloader.png");

    private HbmObjModel cachedModel;

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(RBMKAutoloaderBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        model().renderPart(poseStack, consumer, packedLight, packedOverlay, "Base");

        // CE: `double p = lastPiston + (renderPiston - lastPiston) * partialTicks;` then
        // `translate(0, p * -4D, 0)` followed by an unconditional `translate(0, 4, 0)` - net
        // (p * -4 + 4). Preserved as two separate translates to mirror CE's own call shape exactly.
        float p = Mth.lerp(partialTick, (float) be.lastPiston, (float) be.piston);
        poseStack.translate(0.0, p * -4.0, 0.0);
        poseStack.translate(0.0, 4.0, 0.0);

        model().renderPart(poseStack, consumer, packedLight, packedOverlay, "Piston");

        poseStack.popPose();
    }

    /** CE: {@code TileEntityRBMKAutoloader.getMaxRenderDistanceSquared() -> 65536.0D} (256 blocks) - see {@link RBMKControlRodRenderer#getViewDistance()}'s own javadoc for the confirmed 1.21.1 shape. */
    @Override
    public int getViewDistance() {
        return 256;
    }

    public static final class Provider implements BlockEntityRendererProvider<RBMKAutoloaderBlockEntity> {
        @Override
        public BlockEntityRenderer<RBMKAutoloaderBlockEntity> create(BlockEntityRendererProvider.Context context) {
            return new RBMKAutoloaderPistonRenderer();
        }
    }
}
