package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKControlMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Manual control rod screen - extraction level readout. See {@link RBMKRodScreen} for the texture-asset note. */
public class RBMKControlScreen extends GuiInfoContainer<RBMKControlMenu> {

    public RBMKControlScreen(RBMKControlMenu menu, Inventory inventory, Component title) {
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
        guiGraphics.drawString(this.font,
                "Extraction: " + (int) (getMenu().be.getMult() * 100) + "% (target " + (int) (getMenu().be.targetLevel * 100) + "%)",
                leftPos + 8, topPos + 20, 0x404040, false);
        guiGraphics.drawString(this.font,
                "Color: " + (getMenu().be.color != null ? getMenu().be.color : "none"),
                leftPos + 8, topPos + 32, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
