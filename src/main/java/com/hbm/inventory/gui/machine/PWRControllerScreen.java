package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.PWRControllerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PWRControllerScreen extends GuiInfoContainer<PWRControllerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_pwr.png");

    public PWRControllerScreen(PWRControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int containerId = this.getMenu().containerId;
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, PWRControllerMenu.BUTTON_ROD_DOWN)
        ).bounds(leftPos + 8, topPos + 17, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, PWRControllerMenu.BUTTON_ROD_UP)
        ).bounds(leftPos + 30, topPos + 17, 20, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int p = (int) (be.progress * 33 / be.processTime);
        guiGraphics.blit(TEXTURE, x + 54, y + 33, 176, 0, p, 14);

        int c = (int) (be.rodLevel * 52 / 100);
        guiGraphics.blit(TEXTURE, x + 53, y + 54, 176, 40, c, 2);

        be.tanks[0].renderTank(x + 8, y + 57, 0, 16, 52);
        be.tanks[1].renderTank(x + 26, y + 57, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
        
        var be = this.getMenu().be;
        String flux = String.format("%.1f", be.flux);
        guiGraphics.drawString(this.font, flux, 165 - this.font.width(flux), 64, 0x00FF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 115, topPos + 31, 18, 18, mouseX, mouseY,
                Component.literal("Core: " + be.coreHeat + " / " + be.coreHeatCapacity + " TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 151, topPos + 31, 18, 18, mouseX, mouseY,
                Component.literal("Hull: " + be.hullHeat + " TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 31, 36, 18, mouseX, mouseY,
                Component.literal(((int) (be.progress * 100 / be.processTime)) + "%"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 53, 54, 4, mouseX, mouseY,
                Component.literal("Control rod level: " + (100 - (int) be.rodLevel) + "%"));
        
        be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 5, 16, 52);
        be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 5, 16, 52);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
