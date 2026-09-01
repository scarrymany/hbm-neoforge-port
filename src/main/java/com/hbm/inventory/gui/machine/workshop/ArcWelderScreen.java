package com.hbm.inventory.gui.machine.workshop;

import com.hbm.inventory.container.machine.workshop.ArcWelderMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ArcWelderScreen extends GuiInfoContainer<ArcWelderMenu> {

    public ArcWelderScreen(ArcWelderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        this.getMenu().be.tank.renderTank(x + 152, y + 16, 0, 16, 70);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 140, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 20, 140, 10,
                Component.literal(be.isProcessing ? "Welding" : "Idle"),
                Component.literal("Progress: " + be.getProgressScaled(100) + "%"));
    }
}
