package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.RadarMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Contact count + power. CE map GUI ({@code GUIMachineRadarNT}) not ported. */
public class RadarScreen extends GuiInfoContainer<RadarMenu> {

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 212;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        int power = (int) (88L * this.getMenu().be.getPower() / Math.max(1, this.getMenu().be.getMaxPower()));
        guiGraphics.fill(x + 8, y + 18 + (88 - power), x + 24, y + 18 + 88, 0xFFFF0000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, "Contacts: " + this.getMenu().be.getContacts(), 32, 20, 0x404040, false);
        guiGraphics.drawString(this.font, "Redstone: " + this.getMenu().be.getRedPower(), 32, 32, 0x404040, false);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 88, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
    }
}
