package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKHeaterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RBMKHeaterScreen extends GuiInfoContainer<RBMKHeaterMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_rbmk_heater.png");

    public RBMKHeaterScreen(RBMKHeaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        // Feed tank (cold coolant) at left
        int i = be.feed.getFill() * 58 / Math.max(1, be.feed.getMaxFill());
        guiGraphics.blit(TEXTURE, x + 126, y + 82 - i, 176, 58 - i, 14, i);

        // Steam tank (hot coolant) at center
        int j = be.steam.getFill() * 22 / Math.max(1, be.steam.getMaxFill());
        if (j > 0) j++;
        if (j > 22) j++;
        guiGraphics.blit(TEXTURE, x + 91, y + 65 - j, 190, 24 - j, 4, j);
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
        
        var be = getMenu().be;
        be.feed.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 126, topPos + 24, 16, 56);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 89, topPos + 39, 8, 28);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
