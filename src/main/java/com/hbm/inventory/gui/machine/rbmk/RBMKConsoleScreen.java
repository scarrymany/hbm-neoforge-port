package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKConsoleMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** RBMK console screen - aggregate flux history readout. See {@link RBMKRodScreen} for the texture-asset note. */
public class RBMKConsoleScreen extends GuiInfoContainer<RBMKConsoleMenu> {

    public RBMKConsoleScreen(RBMKConsoleMenu menu, Inventory inventory, Component title) {
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
        int[] buf = getMenu().be.fluxBuffer;
        int last = buf.length > 0 ? buf[buf.length - 1] : 0;
        guiGraphics.drawString(this.font, "Reactor flux: " + last, leftPos + 8, topPos + 20, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
