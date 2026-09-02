package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKControlAutoMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RBMKControlAutoScreen extends GuiInfoContainer<RBMKControlAutoMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_rbmk_control_auto.png");

    public RBMKControlAutoScreen(RBMKControlAutoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
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
        guiGraphics.drawString(this.font, "Function: " + be.function, leftPos + 8, topPos + 20, 0x404040, false);
        guiGraphics.drawString(this.font, "Heat range: " + (int) be.heatLower + " - " + (int) be.heatUpper, leftPos + 8, topPos + 32, 0x404040, false);
        guiGraphics.drawString(this.font, "Level range: " + (int) be.levelLower + " - " + (int) be.levelUpper, leftPos + 8, topPos + 44, 0x404040, false);
        guiGraphics.drawString(this.font, "Target: " + (int) (be.targetLevel * 100) + "%", leftPos + 8, topPos + 56, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
