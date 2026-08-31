package com.hbm.client.render.blockentity.rbmk;

import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlManualBlockEntity;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Control-rod extraction-depth renderer, ported from CE's {@code RenderRBMKControlRod}
 * ({@code upstream/hbm-ce/.../render/tileentity/RenderRBMKControlRod.java}, 67 lines, read in
 * full). Backs both {@link RBMKControlManualBlockEntity} and
 * {@link com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity} - CE registers the exact
 * same {@code RenderRBMKControlRod} class against both concrete tile entity classes via two
 * {@code @AutoRegister} annotations on one class; this port does the equivalent by registering
 * {@link Provider} against both this port's separate {@code BlockEntityType}s
 * ({@code RBMKBlockEntities.CONTROL_MANUAL}/{@code .CONTROL_AUTO} - see this task's own
 * {@code wiringSnippets}, since {@code ClientModRegistry.java} is a shared/aggregator file this
 * task may not edit directly).
 *
 * <h2>What CE does that this class replicates</h2>
 * <ul>
 *   <li>Translates a shared "Lid" OBJ part vertically by the live
 *   {@code Mth.lerp(partialTick, lastExtraction, extraction)} interpolated extraction fraction
 *   (CE field names {@code lastLevel}/{@code level} - this port's {@link RBMKControlBlockEntity}
 *   javadoc already documents the {@code level}-to-{@code extraction} rename forced by
 *   {@code BlockEntity} itself owning a {@code level} field for the world in 1.21.1).</li>
 *   <li>Picks one of 7 textures: the 5 {@link RBMKControlManualBlockEntity.RBMKColor}-tagged
 *   variants when a manual rod has an operator color set, the plain "standard" texture for an
 *   uncolored manual rod, or the "auto" texture for every
 *   {@link com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity} (CE:
 *   {@code RenderRBMKControlRod.java:50-55}).</li>
 *   <li><b>Borrows lighting from the block one above the top of the column</b>
 *   ({@code world.getCombinedLight(pos.up(offset + 1), 0)}, CE lines 44-48) instead of using the
 *   packed light this method is handed for the rod's own (buried-inside-a-dark-column) position -
 *   worth preserving exactly, per the research report's own Phase-5-safe scope item 1. The 1.21.1
 *   equivalent, {@link LevelRenderer#getLightColor(net.minecraft.world.level.BlockAndTintGetter,
 *   BlockPos)}, is confirmed real and used for exactly this "packed light at an arbitrary
 *   position" purpose by {@code upstream/neo-edition}'s own compiling
 *   {@code render/entity/effect/RenderFallout.java:147} (cross-checked for API shape only, per
 *   this port's ground rules).</li>
 * </ul>
 *
 * <h2>Assets not yet present</h2>
 * {@code models/rbmk/rbmk_rods.obj} (CE: {@code ResourceManager.rbmk_rods_vbo}) and the 7
 * {@code textures/block/rbmk/rbmk_control*.png} textures below do not exist in
 * {@code src/main/resources} yet - see
 * {@code docs/phase5/reactor_and_explosion_visual_effects.md}'s "Texture/model assets" Deferred-
 * scope entry. This renderer activates the moment they land: {@link HbmObjModel#get} is only ever
 * called lazily from inside {@link #model()}, itself only reachable from {@link #render}, so a
 * missing resource throws {@link com.hbm.render.loader.ModelFormatException} the first time a
 * control rod is actually on screen, never at class-load or registration time (same pattern
 * {@code ExamplePlaceholderBEWLR} already established in {@code com.hbm.client.render.item} for
 * the identical not-yet-shipped-asset situation).
 */
public final class RBMKControlRodRenderer implements BlockEntityRenderer<RBMKControlBlockEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/rbmk/rbmk_rods.obj");

    /** Index matches {@link RBMKControlManualBlockEntity.RBMKColor#ordinal()} (RED, YELLOW, GREEN, BLUE, PURPLE). */
    private static final ResourceLocation[] COLOR_TEXTURES = {
            texture("rbmk_control_red"),
            texture("rbmk_control_yellow"),
            texture("rbmk_control_green"),
            texture("rbmk_control_blue"),
            texture("rbmk_control_purple"),
    };
    private static final ResourceLocation TEX_STANDARD = texture("rbmk_control");
    private static final ResourceLocation TEX_AUTO = texture("rbmk_control_auto");

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/block/rbmk/" + name + ".png");
    }

    // Lazily populated, see class javadoc's "Assets not yet present" section.
    private HbmObjModel cachedModel;

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(RBMKControlBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

        // RBMKDials' ServerLevel parameter is never dereferenced by any accessor body (every dial
        // reads a synced SERVER-type ModConfigSpec value instead) - see RBMKDials' own class
        // javadoc ("passing null is always safe"). This client-only renderer only ever has a
        // ClientLevel in scope, never the real ServerLevel the accessor is typed to accept.
        int offset = RBMKDials.getColumnHeight(null);
        BlockPos pos = be.getBlockPos();

        poseStack.pushPose();
        // BlockEntityRenderer's PoseStack already starts translated to this block entity's own
        // local origin (the 1.21.1 dispatcher's equivalent of CE's TESR (x,y,z) render-relative
        // offset args), so CE's "x + 0.5, y + offset, z + 0.5" becomes a local (0.5, offset, 0.5).
        poseStack.translate(0.5, offset, 0.5);

        int borrowedLight = LevelRenderer.getLightColor(level, pos.above(offset + 1));

        ResourceLocation texture = TEX_AUTO;
        if (be instanceof RBMKControlManualBlockEntity manual) {
            texture = manual.color == null ? TEX_STANDARD : COLOR_TEXTURES[manual.color.ordinal()];
        }

        float extraction = Mth.lerp(partialTick, (float) be.lastExtraction, (float) be.extraction);
        poseStack.translate(0.0, extraction, 0.0);

        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(texture));
        model().renderPart(poseStack, consumer, borrowedLight, packedOverlay, "Lid");

        poseStack.popPose();
    }

    /**
     * CE: {@code TileEntityRBMKControl.getMaxRenderDistanceSquared() -> 65536.0D} (256 blocks,
     * squared) - the 1.21.1 {@link BlockEntityRenderer} equivalent is a plain block-distance int on
     * the renderer itself, confirmed real (and confirmed to use this exact 256 value for other RBMK
     * renderers) via {@code upstream/neo-edition}'s own compiling
     * {@code render/blockentity/BlockEntityRendererNT.java:24}.
     */
    @Override
    public int getViewDistance() {
        return 256;
    }

    public static final class Provider implements BlockEntityRendererProvider<RBMKControlBlockEntity> {
        @Override
        public BlockEntityRenderer<RBMKControlBlockEntity> create(BlockEntityRendererProvider.Context context) {
            return new RBMKControlRodRenderer();
        }
    }
}
