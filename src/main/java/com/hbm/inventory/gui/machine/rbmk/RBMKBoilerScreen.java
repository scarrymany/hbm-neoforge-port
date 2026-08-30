package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKBoilerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Boiler screen - feed/steam tank readout. See {@link RBMKRodScreen} for the texture-asset note. */
public class RBMKBoilerScreen extends GuiInfoContainer<RBMKBoilerMenu> {

    public RBMKBoilerScreen(RBMKBoilerMenu menu, Inventory inventory, Component title) {
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
        guiGraphics.drawString(this.font, "Water: " + be.feed.getFill() + "/" + be.feed.getMaxFill() + "mB", leftPos + 8, topPos + 20, 0x404040, false);
        guiGraphics.drawString(this.font, "Steam: " + be.steam.getFill() + "/" + be.steam.getMaxFill() + "mB", leftPos + 8, topPos + 32, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
