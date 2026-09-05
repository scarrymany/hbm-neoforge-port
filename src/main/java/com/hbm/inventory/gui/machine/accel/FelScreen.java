package com.hbm.inventory.gui.machine.accel;

import com.hbm.inventory.container.machine.accel.FelMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FelScreen extends GuiInfoContainer<FelMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/machine/gui_fel.png");

    public FelScreen(FelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 203;
        this.imageHeight = 169;
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 142, y + 41, 203, 0, 29, 17);
        }

        int k = (int) (be.getPower() * 113 / Math.max(1, be.getMaxPower()));
        guiGraphics.blit(TEXTURE, x + 182, y + 27 + 113 - k, 203, 17 + 113 - k, 16, k);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 90 + this.imageWidth / 2 - this.font.width(name) / 2, 7, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 98, 0x404040, false);
        
        var be = this.getMenu().be;
        if (be.isOn) {
            String status = be.isOn ? "LIVE" : "";
            guiGraphics.drawString(this.font, status, 54 + this.imageWidth / 2 - this.font.width(name) / 2, 9, 0x00FF00, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 182, topPos + 27, 16, 113, be.getPower(), be.getMaxPower());
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
