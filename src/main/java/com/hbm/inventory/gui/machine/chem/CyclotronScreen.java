package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.CyclotronMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CyclotronScreen extends GuiInfoContainer<CyclotronMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/machine/gui_cyclotron.png");

    public CyclotronScreen(CyclotronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 190;
        this.imageHeight = 215;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int k = (int) (be.getPower() * 63 / Math.max(1, be.getMaxPower()));
        guiGraphics.blit(TEXTURE, x + 168, y + 80 - k, 190, 62 - k, 16, k);

        int l = be.getProgressScaled(34);
        guiGraphics.blit(TEXTURE, x + 48, y + 27, 206, 0, l, 34);

        if (l > 0) {
            guiGraphics.blit(TEXTURE, x + 172, y + 4, 190, 63, 9, 12);
        }

        be.tanks[0].renderTank(x + 11, y + 88, 0, 34, 7);
        be.tanks[1].renderTank(x + 11, y + 97, 0, 34, 7);
        be.tanks[2].renderTank(x + 107, y + 97, 0, 34, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 79 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 15, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 168, topPos + 18, 16, 63, be.getPower(), be.getMaxPower());
        
        be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 11, topPos + 81, 34, 7);
        be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 11, topPos + 90, 34, 7);
        be.tanks[2].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 107, topPos + 81, 34, 16);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
