package com.hbm.inventory.gui.machine.chem;

import com.hbm.blockentity.machine.chem.GasCentrifugeBlockEntity;
import com.hbm.inventory.container.machine.chem.GasCentrifugeMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes.PseudoFluidType;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Arrays;

/**
 * Exact CE {@code GUIMachineGasCent} on existing {@code gui_centrifuge_gas.png} 206×204.
 * Power 182,69-h / progress 70,35 / tanks 16,32 + 138,154. Info panels −12,16 / −12,32.
 */
public class GasCentrifugeScreen extends GuiInfoContainer<GasCentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_centrifuge_gas.png");

    public GasCentrifugeScreen(GasCentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 206;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        GasCentrifugeBlockEntity be = this.getMenu().be;
        // CE GUIMachineGasCent.java:84-91
        int powerHeight = (int) be.getPowerRemainingScaled(52);
        if (powerHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 182, y + 69 - powerHeight, 206, 52 - powerHeight, 16, powerHeight);
        }
        int progressWidth = be.getCentrifugeProgressScaled(36);
        if (progressWidth > 0) {
            guiGraphics.blit(TEXTURE, x + 70, y + 35, 206, 52, progressWidth, 13);
        }

        // CE :95-102 — real-tank texture, pseudo fill
        renderTank(x + 16, y + 16, 6, 52, be.inputTank.getFill(), be.inputTank.getMaxFill());
        renderTank(x + 32, y + 16, 6, 52, be.inputTank.getFill(), be.inputTank.getMaxFill());
        renderTank(x + 138, y + 16, 6, 52, be.outputTank.getFill(), be.outputTank.getMaxFill());
        renderTank(x + 154, y + 16, 6, 52, be.outputTank.getFill(), be.outputTank.getMaxFill());

        // CE :105-107
        drawInfoPanel(guiGraphics, x - 12, y + 16, 3);
        drawInfoPanel(guiGraphics, x - 12, y + 32, 2);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :67-68 — inventory label only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        GasCentrifugeBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 182, topPos + 17, 16, 52, be.power, be.getMaxPower());

        Component inName = tankName(be.inputTank.getTankType());
        if (be.inputTank.getTankType().getIfHighSpeed()) {
            // CE :45-46: processingSpeed (static 150) > processingSpeed - 70 → always DARK_RED
            inName = inName.copy().withStyle(ChatFormatting.DARK_RED);
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 15, topPos + 15, 24, 55, mouseX, mouseY,
                inName,
                Component.literal(be.inputTank.getFill() + " / " + be.inputTank.getMaxFill() + " mB"));

        Component outName = tankName(be.outputTank.getTankType());
        if (be.outputTank.getTankType().getIfHighSpeed()) {
            outName = outName.copy().withStyle(ChatFormatting.GOLD);
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 137, topPos + 15, 25, 55, mouseX, mouseY,
                outName,
                Component.literal(be.outputTank.getFill() + " / " + be.outputTank.getMaxFill() + " mB"));

        Component[] enrichment = Arrays.stream(I18nUtil.resolveKeyArray("desc.gui.gasCent.enrichment"))
                .map(Component::literal).toArray(Component[]::new);
        Component[] output = Arrays.stream(I18nUtil.resolveKeyArray("desc.gui.gasCent.output"))
                .map(Component::literal).toArray(Component[]::new);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 12, topPos + 16, 16, 16,
                leftPos - 8, topPos + 32, enrichment);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 12, topPos + 32, 16, 16,
                leftPos - 8, topPos + 48, output);
    }

    private static Component tankName(PseudoFluidType type) {
        return Component.literal(I18nUtil.resolveKey(type.getTranslationKey()));
    }

    /**
     * Exact CE {@code GUIMachineGasCent.renderTank} :110-137: texture from the real feed tank,
     * fill from the pseudo tanks. y is the top of the column (CE then {@code y += height}).
     */
    private void renderTank(int x, int y, int width, int height, int fluid, int maxFluid) {
        if (maxFluid <= 0 || fluid <= 0) return;
        int scaledHeight = (fluid * height) / maxFluid;
        if (scaledHeight <= 0) return;

        FluidType type = this.getMenu().be.tank.getTankType();
        y += height;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, type.getTexture());

        float minX = x;
        float maxX = x + width;
        float minY = y - height;
        float maxY = y - (height - scaledHeight);
        float minU = 0F;
        float maxU = width / 16F;
        float minV = 1F;
        float maxV = 1F - scaledHeight / 16F;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(minX, maxY, 0).setUv(minU, maxV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        buf.addVertex(maxX, maxY, 0).setUv(maxU, maxV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        buf.addVertex(maxX, minY, 0).setUv(maxU, minV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        buf.addVertex(minX, minY, 0).setUv(minU, minV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
