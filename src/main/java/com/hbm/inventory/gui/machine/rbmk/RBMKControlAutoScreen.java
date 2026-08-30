package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKControlAutoMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Auto control rod screen - heat/level bound readout. See {@link RBMKRodScreen} for the texture-asset note. */
public class RBMKControlAutoScreen extends GuiInfoContainer<RBMKControlAutoMenu> {

    public RBMKControlAutoScreen(RBMKControlAutoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0C6C6C6);
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
