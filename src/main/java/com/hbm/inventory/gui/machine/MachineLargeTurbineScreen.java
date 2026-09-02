package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineLargeTurbineMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MachineLargeTurbineScreen extends GuiInfoContainer<MachineLargeTurbineMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/generators/gui_turbine_large.png");

    public MachineLargeTurbineScreen(MachineLargeTurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int p = (int) (be.getPower() * 34 / Math.max(1, be.getMaxPower()));
        guiGraphics.blit(TEXTURE, x + 123, y + 69 - p, 176, 34 - p, 7, p);

        be.tanks[0].renderTank(x + 62, y + 69, 0, 16, 52);
        be.tanks[1].renderTank(x + 134, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 123, topPos + 69 - 34, 7, 34, be.getPower(), be.getMaxPower());
        be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 69 - 52, 16, 52);
        be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 69 - 52, 16, 52);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
