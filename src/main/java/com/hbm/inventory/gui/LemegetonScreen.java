package com.hbm.inventory.gui;

import com.hbm.inventory.container.LemegetonMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUILemegeton} — 1-in/1-out conversion. Fill-panel (CE texture not in this tree).
 */
public class LemegetonScreen extends GuiInfoContainer<LemegetonMenu> {

    public LemegetonScreen(LemegetonMenu menu, Inventory inventory, Component title) {
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
        guiGraphics.drawString(this.font, "Material Upgrade Conversion", leftPos + 28, topPos + 6, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
