package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.WatzReactorMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUIWatz}) as a plain panel. */
public class WatzReactorScreen extends GuiInfoContainer<WatzReactorMenu> {

    public WatzReactorScreen(WatzReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 205 + 58;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        var be = this.getMenu().be;
        be.tanks[0].renderTank(x + 118, y + 8, 0, 16, 90);
        be.tanks[1].renderTank(x + 138, y + 8, 0, 16, 90);
        be.tanks[2].renderTank(x + 158, y + 8, 0, 16, 90);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 108, 100, 12,
                Component.literal("Heat: " + (int) be.heat));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 122, 100, 12,
                Component.literal("Flux: " + (int) be.fluxDisplay));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 136, 100, 12,
                Component.literal(be.isOn ? "Status: ON" : "Status: OFF"));
    }
}
