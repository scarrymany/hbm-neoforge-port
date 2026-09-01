package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.AshpitMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIAshpit} — 5-slot ash tray. */
public class AshpitScreen extends GuiInfoContainer<AshpitMenu> {

    public AshpitScreen(AshpitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF5A4030);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF8A7060);
        guiGraphics.fill(x + 40, y + 22, x + 138, y + 48, 0xFF3A2818);
    }
}
