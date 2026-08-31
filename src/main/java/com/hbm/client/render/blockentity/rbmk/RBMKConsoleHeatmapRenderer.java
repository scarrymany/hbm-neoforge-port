package com.hbm.client.render.blockentity.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKColumn;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * In-world 15x15 reactor heatmap, ported from the heatmap-drawing half of CE's
 * {@code RenderRBMKConsole} ({@code upstream/hbm-ce/.../render/tileentity/RenderRBMKConsole.java},
 * lines 56-249 read in full - the remaining ~120 of 250 lines are the console's own OBJ-model
 * shell/screen-frame draw and the flux-graph {@code FontRenderer} text overlay, both out of this
 * task's scope per the research report: this class draws only the colored-quad heatmap layer that
 * sits on top of the shell, which this port's {@code RBMKConsoleBlock} already renders as an
 * ordinary baked block model ({@code RenderShape.MODEL}), not a TESR/BER concern).
 *
 * <h2>What this renders, per {@link RBMKConsoleBlockEntity#columns} entry</h2>
 * <ul>
 *   <li>One base tile quad ({@code drawColumn}) per non-null column, tinted white by default;
 *   {@code CONTROL}-type columns instead use either a solid color (when
 *   {@link RBMKColumn.ControlColumn#color} is set, 5 CE-literal RGB combinations matching
 *   {@link com.hbm.blockentity.machine.rbmk.RBMKControlManualBlockEntity.RBMKColor}'s ordinal
 *   order) or the heat-escalation formula {@code r = colorValue + (1 - colorValue) * (heat /
 *   maxHeat)} with a faint per-index-parity alternating base ({@code colorValue = 0.65 + (i % 2) *
 *   0.05}) when uncolored - the exact math
 *   {@code docs/phase5/reactor_and_explosion_visual_effects.md}'s Phase-5-safe scope item 3 cites.
 *   A lit {@code col.indicator} overrides the tile to solid yellow regardless of type, matching
 *   CE's own after-the-fact override ordering.</li>
 *   <li>A small secondary diamond marker ({@code drawDot}, CE's own additional readout layer, read
 *   in full alongside {@code drawColumn} even though the task's own one-line scope description
 *   only names the base tile grid) for {@code FUEL}/{@code FUEL_SIM} columns (green, brightness by
 *   enrichment), {@code CONTROL} columns (red-green by extraction level), and
 *   {@code CONTROL_AUTO} columns (red-blue by extraction level) - included because it is drawn
 *   from the exact same {@code columns[]} data this class already reads, costs one more manual
 *   quad batch, and is squarely part of what CE's own screen calls its heatmap; flagged in this
 *   task's own notes in case the coordinator wants it split out.</li>
 * </ul>
 *
 * <h2>Known gap: no console facing/rotation data</h2>
 * CE reads the console's placement direction off {@code getBlockMetadata() - BlockDummyable.offset}
 * and rotates the whole heatmap draw to match (0/90/180/270 degrees). This port's
 * {@code RBMKConsoleBlock} (block-registration scope owned by a different task) is a plain
 * {@code BaseEntityBlock} with no {@code Direction}/{@code HorizontalDirectionalBlock} property at
 * all yet - confirmed by grep, no {@code FACING} anywhere under
 * {@code com.hbm.blocks.machine.rbmk}. This renderer therefore always draws in CE's own unrotated
 * ("EAST"/metadata-5) orientation; see this task's own notes for the follow-up once a facing
 * property exists.
 *
 * <h2>Manual immediate-mode draw, not a batched {@code RenderType}</h2>
 * Same rationale as {@link RBMKFuelColumnRenderer}'s Cherenkov glow: a plain opaque untextured
 * position+color quad batch has no standard vanilla {@code RenderType} to reuse, so this class
 * draws via {@link Tesselator}/{@link BufferBuilder}/{@link BufferUploader#drawWithShader}
 * directly (API shape confirmed real via {@code upstream/neo-edition}'s own compiling
 * {@code render/util/RenderInfoSystem.java:89-97}, cross-checked for shape only). Cull is
 * deliberately disabled for this batch (unlike CE, which leaves {@code GL_CULL_FACE} enabled here -
 * see {@link #render}'s own comment) since this sandbox cannot verify vertex winding visually and a
 * culled-invisible heatmap is a strictly worse failure mode than a double-sided one.
 */
public final class RBMKConsoleHeatmapRenderer implements BlockEntityRenderer<RBMKConsoleBlockEntity> {

    private static final float TILE_WIDTH = 0.0625F * 0.75F; // CE: RenderRBMKConsole.drawColumn's `width`
    private static final float DOT_WIDTH = 0.03125F; // CE: RenderRBMKConsole.drawDot's `width`
    private static final float DOT_EDGE = 0.022097F; // CE: RenderRBMKConsole.drawDot's `edge`

    @Override
    public void render(RBMKConsoleBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // CE translates (x+0.5,y,z+0.5), rotates by placement facing, then translates (0.5,0,0)
        // again - net (1.0, 0, 0.5) before any rotation. See class javadoc's "Known gap" section for
        // why no rotation is applied here.
        poseStack.translate(1.0, 0.0, 0.5);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        Matrix4f mat = poseStack.last().pose();
        BufferBuilder buffer = null;

        for (int i = 0; i < be.columns.length; i++) {
            RBMKColumn col = be.columns[i];
            if (col == null) continue;
            if (buffer == null) {
                buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            }

            float kx = -0.3725F;
            float ky = -(float) Math.floor(i / 15F) * 0.125F + 3.625F;
            float kz = -(i % 15) * 0.125F + 0.125F * 7F;

            float r = 1.0F, g = 1.0F, b = 1.0F;
            if (col.type == RBMKColumn.ColumnType.CONTROL) {
                RBMKColumn.ControlColumn control = (RBMKColumn.ControlColumn) col;
                if (control.color >= 0) {
                    // CE: RenderRBMKConsole.render's 5-way `colorType` switch (RED, YELLOW, GREEN,
                    // BLUE, PURPLE - matching RBMKControlManualBlockEntity.RBMKColor's ordinal order).
                    switch (control.color) {
                        case 0 -> { g = 0F; b = 0F; }
                        case 1 -> b = 0F;
                        case 2 -> { r = 0F; g = 0.5F; b = 0F; }
                        case 3 -> { r = 0F; g = 0F; }
                        case 4 -> { r = 0.5F; g = 0F; }
                        default -> { }
                    }
                } else {
                    double heat = col.maxHeat <= 0 ? 0D : col.heat / col.maxHeat;
                    double colorValue = 0.65D + (i % 2) * 0.05D;
                    r = (float) (colorValue + (1 - colorValue) * heat);
                    g = (float) colorValue;
                    b = (float) colorValue;
                }
            }

            if (col.indicator > 0) {
                r = 1.0F;
                g = 1.0F;
                b = 0.0F;
            }

            addQuad(buffer, mat, kx, ky, kz, TILE_WIDTH, r, g, b);

            switch (col.type) {
                case FUEL, FUEL_SIM -> {
                    float enrichment = (float) ((RBMKColumn.FuelColumn) col).enrichment;
                    addDot(buffer, mat, kx + 0.01F, ky, kz, 0F, 0.25F + enrichment * 0.75F, 0F);
                }
                case CONTROL -> {
                    float level = (float) ((RBMKColumn.ControlColumn) col).level;
                    addDot(buffer, mat, kx + 0.01F, ky, kz, level, level, 0F);
                }
                case CONTROL_AUTO -> {
                    float level = (float) ((RBMKColumn.ControlColumn) col).level;
                    addDot(buffer, mat, kx + 0.01F, ky, kz, level, 0F, level);
                }
                default -> {
                }
            }
        }

        if (buffer != null) {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        RenderSystem.enableCull();
        poseStack.popPose();
    }

    /** CE: {@code TileEntityRBMKConsole.getMaxRenderDistanceSquared() -> 65536.0D} (256 blocks) - see {@link RBMKControlRodRenderer#getViewDistance()}'s own javadoc for the confirmed 1.21.1 shape. */
    @Override
    public int getViewDistance() {
        return 256;
    }

    /** CE: {@code RenderRBMKConsole.drawColumn} - a flat quad on the {@code x = kx} plane. */
    private static void addQuad(BufferBuilder buffer, Matrix4f mat, float x, float y, float z, float width,
                                 float r, float g, float b) {
        buffer.addVertex(mat, x, y + width, z - width).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y + width, z + width).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - width, z + width).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - width, z - width).setColor(r, g, b, 1.0F);
    }

    /** CE: {@code RenderRBMKConsole.drawDot} - a 3-quad diamond marker, slightly proud of the base tile. */
    private static void addDot(BufferBuilder buffer, Matrix4f mat, float x, float y, float z,
                                float r, float g, float b) {
        buffer.addVertex(mat, x, y + DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y + DOT_EDGE, z + DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y, z + DOT_WIDTH).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z + DOT_EDGE).setColor(r, g, b, 1.0F);

        buffer.addVertex(mat, x, y + DOT_EDGE, z - DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y + DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z - DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y, z - DOT_WIDTH).setColor(r, g, b, 1.0F);

        buffer.addVertex(mat, x, y + DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z + DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z - DOT_EDGE).setColor(r, g, b, 1.0F);
    }

    public static final class Provider implements BlockEntityRendererProvider<RBMKConsoleBlockEntity> {
        @Override
        public BlockEntityRenderer<RBMKConsoleBlockEntity> create(BlockEntityRendererProvider.Context context) {
            return new RBMKConsoleHeatmapRenderer();
        }
    }
}
