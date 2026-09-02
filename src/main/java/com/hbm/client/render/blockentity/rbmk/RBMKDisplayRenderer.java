package com.hbm.client.render.blockentity.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKColumn;
import com.hbm.blockentity.machine.rbmk.RBMKDisplayBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBlock;
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
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/**
 * CE: com.hbm.render.tileentity.RenderRBMKDisplay (lines 1-173)
 * Renders a 7×7 grid of colored 3D column indicators on the RBMK display panel face.
 * Each column shows heat/type status via color + optional fuel/control markers (small diamond dots).
 *
 * <h2>What this renders per {@link RBMKDisplayBlockEntity#columns} entry</h2>
 * <ul>
 *   <li>One base column quad per non-null column, colored by type/heat:
 *     <ul>
 *       <li>{@code CONTROL}-type with {@link RBMKColumn.ControlColumn#color} set: solid color
 *           (5 CE RGB values - RED, YELLOW, GREEN, BLUE, PURPLE)</li>
 *       <li>Otherwise: heat-gradient from base gray to red ({@code r = base + (1 - base) * heat})</li>
 *       <li>Lit {@code col.indicator}: overrides to solid yellow</li>
 *     </ul>
 *   </li>
 *   <li>Secondary diamond marker ({@code drawDot}) for:
 *     <ul>
 *       <li>{@code FUEL/FUEL_SIM/BREEDER}: green, brightness by enrichment</li>
 *       <li>{@code CONTROL}: red-green gradient by extraction level</li>
 *       <li>{@code CONTROL_AUTO}: red-blue gradient by extraction level</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Coordinate transform</h2>
 * CE translates (x+0.5, y, z+0.5), rotates by block facing (90°/180°/270°/0° for N/W/S/E),
 * then applies (0, 0.5, 0), scales Y/Z by 8/7, and translates (0, -0.5, 0).
 * Rotation uses the panel's {@link RBMKMiniPanelBlock#FACING} property.
 */
public final class RBMKDisplayRenderer implements BlockEntityRenderer<RBMKDisplayBlockEntity> {

    private static final float TILE_WIDTH = 0.0625F * 0.75F; // CE: width in drawColumn
    private static final float DOT_WIDTH = 0.03125F;         // CE: width in drawDot
    private static final float DOT_EDGE = 0.022097F;         // CE: edge in drawDot

    @Override
    public void render(RBMKDisplayBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // CE: translate(x+0.5, y, z+0.5), rotate by facing, then transform stack
        poseStack.translate(0.5, 0.0, 0.5);

        // Rotation by facing: CE checks block metadata FACING and rotates
        Direction facing = be.getBlockState().getValue(RBMKMiniPanelBlock.FACING);
        switch (facing) {
            case NORTH -> poseStack.mulPose(org.joml.Quaternionf.fromAxisAngleDeg(0F, 1F, 0F, 90F));
            case WEST -> poseStack.mulPose(org.joml.Quaternionf.fromAxisAngleDeg(0F, 1F, 0F, 180F));
            case SOUTH -> poseStack.mulPose(org.joml.Quaternionf.fromAxisAngleDeg(0F, 1F, 0F, 270F));
            case EAST -> { } // 0 degrees
            default -> { }
        }

        // CE: translate(0, 0.5, 0), scale(1, 8/7, 8/7), translate(0, -0.5, 0)
        poseStack.translate(0, 0.5, 0);
        poseStack.scale(1F, 8F / 7F, 8F / 7F);
        poseStack.translate(0, -0.5, 0);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableTexture(); // CE: disableTexture2D
        RenderSystem.disableCull();

        Matrix4f mat = poseStack.last().pose();
        BufferBuilder buffer = null;

        for (int i = 0; i < be.columns.length; i++) {
            RBMKColumn col = be.columns[i];
            if (col == null) continue;

            if (buffer == null) {
                buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            }

            int row = i / 7;
            int column = i % 7;
            float kx = 0.28125F;
            float ky = -row * 0.125F + 0.875F;
            float kz = -column * 0.125F + 0.125F * 3F;

            float r = 1.0F, g = 1.0F, b = 1.0F;

            if (col.type == RBMKColumn.ColumnType.CONTROL) {
                RBMKColumn.ControlColumn control = (RBMKColumn.ControlColumn) col;
                if (control.color >= 0) {
                    // CE: 5 color types - RED, YELLOW, GREEN, BLUE, PURPLE
                    switch (control.color) {
                        case 0 -> { g = 0F; b = 0F; } // RED
                        case 1 -> b = 0F;               // YELLOW
                        case 2 -> { r = 0F; g = 0.5F; b = 0F; } // GREEN
                        case 3 -> { r = 0F; g = 0F; }   // BLUE
                        case 4 -> { r = 0.5F; g = 0F; } // PURPLE
                        default -> { }
                    }
                } else {
                    double heat = col.maxHeat > 0 ? col.heat / col.maxHeat : 0;
                    double baseColor = 0.65D + (i % 2) * 0.05D;
                    r = (float) (baseColor + ((1 - baseColor) * heat));
                    g = (float) baseColor;
                    b = (float) baseColor;
                }
            } else {
                double heat = col.maxHeat > 0 ? col.heat / col.maxHeat : 0;
                double baseColor = 0.65D + (i % 2) * 0.05D;
                r = (float) (baseColor + ((1 - baseColor) * heat));
                g = (float) baseColor;
                b = (float) baseColor;
            }

            if (col.indicator > 0) {
                r = 1.0F;
                g = 1.0F;
                b = 0.0F;
            }

            addColumn(buffer, mat, kx, ky, kz, r, g, b);

            // Secondary markers for fuel/control columns
            switch (col.type) {
                case FUEL, FUEL_SIM, BREEDER -> {
                    if (col instanceof RBMKColumn.FuelColumn fuel) {
                        float enrichment = (float) fuel.enrichment;
                        addDot(buffer, mat, kx + 0.01F, ky, kz, 0F, 0.25F + enrichment * 0.75F, 0F);
                    }
                }
                case CONTROL -> {
                    if (col instanceof RBMKColumn.ControlColumn ctrl) {
                        float level = (float) ctrl.level;
                        addDot(buffer, mat, kx + 0.01F, ky, kz, level, level, 0F);
                    }
                }
                case CONTROL_AUTO -> {
                    if (col instanceof RBMKColumn.ControlColumn ctrl) {
                        float level = (float) ctrl.level;
                        addDot(buffer, mat, kx + 0.01F, ky, kz, level, 0F, level);
                    }
                }
                default -> { }
            }
        }

        if (buffer != null) {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        RenderSystem.enableTexture(); // CE: enableTexture2D
        RenderSystem.enableCull();
        poseStack.popPose();
    }

    /** CE: TileEntityRBMKDisplay renders at extended range (same as console - 256 blocks) */
    @Override
    public int getViewDistance() {
        return 256;
    }

    /** CE: RenderRBMKDisplay.drawColumn - flat quad on x=kx plane */
    private static void addColumn(BufferBuilder buffer, Matrix4f mat, float x, float y, float z,
                                   float r, float g, float b) {
        buffer.addVertex(mat, x, y + TILE_WIDTH, z - TILE_WIDTH).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y + TILE_WIDTH, z + TILE_WIDTH).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - TILE_WIDTH, z + TILE_WIDTH).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - TILE_WIDTH, z - TILE_WIDTH).setColor(r, g, b, 1.0F);
    }

    /** CE: RenderRBMKDisplay.drawDot - 3-quad diamond marker slightly proud of base tile */
    private static void addDot(BufferBuilder buffer, Matrix4f mat, float x, float y, float z,
                                float r, float g, float b) {
        // Quad 1
        buffer.addVertex(mat, x, y + DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y + DOT_EDGE, z + DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y, z + DOT_WIDTH).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z + DOT_EDGE).setColor(r, g, b, 1.0F);

        // Quad 2
        buffer.addVertex(mat, x, y + DOT_EDGE, z - DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y + DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z - DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y, z - DOT_WIDTH).setColor(r, g, b, 1.0F);

        // Quad 3
        buffer.addVertex(mat, x, y + DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z + DOT_EDGE).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_WIDTH, z).setColor(r, g, b, 1.0F);
        buffer.addVertex(mat, x, y - DOT_EDGE, z - DOT_EDGE).setColor(r, g, b, 1.0F);
    }

    public static final class Provider implements BlockEntityRendererProvider<RBMKDisplayBlockEntity> {
        @Override
        public BlockEntityRenderer<RBMKDisplayBlockEntity> create(BlockEntityRendererProvider.Context context) {
            return new RBMKDisplayRenderer();
        }
    }
}
