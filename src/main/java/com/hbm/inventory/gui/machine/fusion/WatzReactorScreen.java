package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.WatzReactorMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WatzReactorScreen extends GuiInfoContainer<WatzReactorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_watz.png");

    public WatzReactorScreen(WatzReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 229;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 131, 122);
        guiGraphics.blit(TEXTURE, x + 131, y, 131, 0, 36, 122);
        guiGraphics.blit(TEXTURE, x, y + 130, 0, 130, this.imageWidth, 99);
        guiGraphics.blit(TEXTURE, x + 126, y + 31, 176, 31, 9, 60);
        guiGraphics.blit(TEXTURE, x + 105, y + 96, 185, 26, 30, 26);
        guiGraphics.blit(TEXTURE, x + 9, y + 96, 184, 0, 26, 26);

        var be = this.getMenu().be;
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 147, y + 8, 176, 0, 8, 8);
        }

        be.tanks[0].renderTank(x + 143, y + 69, 0, 4, 43);
        be.tanks[1].renderTank(x + 149, y + 69, 0, 4, 43);
        be.tanks[2].renderTank(x + 155, y + 69, 0, 4, 43);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 93, 0x404040, false);
        
        var be = this.getMenu().be;
        String flux = String.format("%.1f", be.fluxDisplay);
        guiGraphics.drawString(this.font, flux, 161 - this.font.width(flux), 107, 0x00FF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 13, topPos + 100, 18, 18, mouseX, mouseY,
                Component.literal(be.heat + " TU"));
        
        be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 142, topPos + 23, 6, 45);
        be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 148, topPos + 23, 6, 45);
        be.tanks[2].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 154, topPos + 23, 6, 45);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
