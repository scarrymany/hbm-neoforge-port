package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.DiFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DiFurnaceScreen extends GuiInfoContainer<DiFurnaceMenu> {

    public DiFurnaceScreen(DiFurnaceMenu menu, Inventory inventory, Component title) {
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

        MachineDiFurnaceBlockEntity be = this.getMenu().be;
        int fh = be.fuel * 52 / MachineDiFurnaceBlockEntity.MAX_FUEL;
        guiGraphics.fill(x + 26, y + 70 - fh, x + 42, y + 70, 0xFFFF6622);
        int ph = be.progress * 24 / MachineDiFurnaceBlockEntity.PROCESS_TICKS;
        guiGraphics.fill(x + 101, y + 35, x + 101 + ph, y + 49, 0xFFFFFF55);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineDiFurnaceBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52,
                Component.literal("Fuel: " + be.fuel + " / " + MachineDiFurnaceBlockEntity.MAX_FUEL));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 101, topPos + 35, 24, 14,
                Component.literal(be.progress + " / " + MachineDiFurnaceBlockEntity.PROCESS_TICKS));
    }
}
