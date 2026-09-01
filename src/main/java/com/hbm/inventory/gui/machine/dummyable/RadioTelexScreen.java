package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.RadioTelexBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadioTelexMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Channel inspect (CE full telex editor is screen-only + packets). */
public class RadioTelexScreen extends GuiInfoContainer<RadioTelexMenu> {

    public RadioTelexScreen(RadioTelexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A2A20);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF4A4A38);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        RadioTelexBlockEntity be = this.getMenu().be;
        guiGraphics.drawString(this.font, "TX " + be.txChannel, 8, 18, 0xA0FFA0, false);
        guiGraphics.drawString(this.font, "RX " + be.rxChannel, 8, 28, 0xA0A0FF, false);
    }
}
