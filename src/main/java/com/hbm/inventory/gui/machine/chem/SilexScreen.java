package com.hbm.inventory.gui.machine.chem;

import com.hbm.blockentity.machine.chem.SilexBlockEntity;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.machine.chem.SilexMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.chem.SILEXRecipes;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Exact CE {@code GUISILEX} on existing {@code gui_silex.png} 176×222.
 * Acid frame 7,41 / fluid 8,42 / progress 45,82 / charge 26,124-f / wave 81,46 / void 10,92.
 */
public class SilexScreen extends GuiInfoContainer<SilexMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_silex.png");

    public SilexScreen(SilexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        SilexBlockEntity be = this.getMenu().be;
        // CE GUISILEX.java:86-102
        if (be.tank.getFill() > 0) {
            guiGraphics.blit(TEXTURE, x + 7, y + 41, 176, acceptedFluid(be) ? 118 : 109, 54, 9);
        }
        int p = be.getProgressScaled(69);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 45, y + 82, 176, 0, p, 43);
        }
        int f = be.getFillScaled(52);
        if (f > 0) {
            guiGraphics.blit(TEXTURE, x + 26, y + 124 - f, 176, 109 - f, 16, f);
        }
        int i = be.getFluidScaled(52);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 42, 176, be.tank.getTankType() == Fluids.PEROXIDE ? 43 : 50, i, 7);
        }
        if (be.mode != EnumWavelengths.NULL && be.getLevel() != null) {
            float freq = 0.0125F * (float) Math.pow(2, be.mode.ordinal());
            int color = be.mode != EnumWavelengths.VISIBLE
                    ? be.mode.guiColor
                    : Mth.hsvToRgb((be.getLevel().getGameTime() / 50.0F) % 1.0F, 0.5F, 1F);
            drawWave(x, y, 81, 46, 16, 84, 0.5F, freq, color, 3F, be.getLevel().getGameTime());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE GUISILEX.java:68-76
        int nameX = (this.imageWidth / 2 - this.font.width(this.title) / 2) - 54;
        guiGraphics.drawString(this.font, this.title, nameX, 8, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        SilexBlockEntity be = this.getMenu().be;
        if (be.mode != EnumWavelengths.NULL) {
            String modeName = I18nUtil.resolveKey(be.mode.name);
            int modeX = 100 + (32 - this.font.width(modeName) / 2);
            guiGraphics.drawString(this.font, Component.literal(modeName).withStyle(be.mode.textColor), modeX, 16, 0xFFFFFF, false);
        }

        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 42, 52, 7);
        if (be.current != null) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 27, topPos + 72, 16, 52, mouseX, mouseY,
                    Component.literal(be.currentFill + "/" + SilexBlockEntity.MAX_FILL + "mB"),
                    be.current.toStack().getHoverName());
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 10, topPos + 92, 10, 10, mouseX, mouseY,
                Component.literal("Void contents"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUISILEX.java:60-64
        if (isHovered(mouseX, mouseY, 10, 92, 12, 12)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, SilexMenu.BUTTON_VOID);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Exact CE {@code GUISILEX.java:88} valid-acid overlay. */
    private static boolean acceptedFluid(SilexBlockEntity be) {
        FluidType type = be.tank.getTankType();
        if (type == Fluids.PEROXIDE || type == Fluids.UF6 || type == Fluids.PUF6 || type == Fluids.DEATH) {
            return true;
        }
        Item icon = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fluid_icon"));
        ItemStack stack = new ComparableStack(icon, 1, type.getID()).toStack();
        return SILEXRecipes.getOutput(stack) != null;
    }

    /** Exact CE {@code GUISILEX.drawWave} :111-135. */
    private static void drawWave(int guiLeft, int guiTop, int x, int y, int height, int width,
                                 float resolution, float freq, int color, float thickness, long gameTime) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.lineWidth(thickness);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float samples = width / resolution;
        float scale = height / 2F;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        float offset = (float) (gameTime % (4 * Math.PI / freq));
        for (int i = 0; i <= samples; i++) {
            double currentX = x + i * resolution;
            double currentY = y + scale * Math.sin((currentX + offset) * freq);
            buf.addVertex((float) (guiLeft + currentX), (float) (guiTop + currentY), 0)
                    .setColor(r, g, b, 1.0F);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.lineWidth(1.0F);
    }
}
