package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.SirenMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineSiren} cassette slot. */
public class SirenScreen extends GuiInfoContainer<SirenMenu> {

    public SirenScreen(SirenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF404050);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF707088);
        guiGraphics.fill(x + 76, y + 32, x + 98, y + 54, 0xFF202028);
    }
}
