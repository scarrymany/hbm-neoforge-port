package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachinePyroOvenBlockEntity;
import com.hbm.inventory.container.machine.dummyable.PyroOvenMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIPyroOven} 176×204 — 2 tanks + power + progress. */
public class PyroOvenScreen extends GuiInfoContainer<PyroOvenMenu> {

    public PyroOvenScreen(PyroOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachinePyroOvenBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 152, y + 70 - ph, x + 168, y + 70, 0xFFFFCC00);
        int p = (int) (be.progress * 27);
        guiGraphics.fill(x + 57, y + 47, x + 57 + p, y + 59, be.isProgressing ? 0xFFFF8800 : 0xFF664400);
        be.input.renderTank(x + 8, y + 70, 0, 16, 52);
        be.output.renderTank(x + 116, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachinePyroOvenBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 18, 16, 52, be.getPower(), be.getMaxPower());
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 57, topPos + 47, 27, 12,
                Component.literal(be.isProgressing ? "Pyrolyzing" : "Idle"),
                Component.literal(String.format("%.0f%%", be.progress * 100)));
    }
}
