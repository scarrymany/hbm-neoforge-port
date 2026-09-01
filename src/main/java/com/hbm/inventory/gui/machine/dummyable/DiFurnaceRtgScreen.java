package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceRtgBlockEntity;
import com.hbm.inventory.container.machine.dummyable.DiFurnaceRtgMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DiFurnaceRtgScreen extends GuiInfoContainer<DiFurnaceRtgMenu> {

    public DiFurnaceRtgScreen(DiFurnaceRtgMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineDiFurnaceRtgBlockEntity be = this.getMenu().be;
        int hh = be.heat * 52 / MachineDiFurnaceRtgBlockEntity.MAX_HEAT;
        guiGraphics.fill(x + 62, y + 70 - hh, x + 70, y + 70, 0xFF88FF44);
        int ph = be.progress * 24 / MachineDiFurnaceRtgBlockEntity.PROCESS;
        guiGraphics.fill(x + 101, y + 35, x + 101 + ph, y + 49, 0xFFFFFF55);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineDiFurnaceRtgBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 18, 8, 52,
                Component.literal("Heat: " + be.heat + " / " + MachineDiFurnaceRtgBlockEntity.MAX_HEAT));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 101, topPos + 35, 24, 14,
                Component.literal(be.progress + " / " + MachineDiFurnaceRtgBlockEntity.PROCESS));
    }
}
