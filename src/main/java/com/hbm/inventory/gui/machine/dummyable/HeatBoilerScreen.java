package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeatBoilerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HeatBoilerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tank + heat inspect for heat boilers. */
public class HeatBoilerScreen extends GuiInfoContainer<HeatBoilerMenu> {

    public HeatBoilerScreen(HeatBoilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        HeatBoilerBlockEntity be = this.getMenu().be;
        be.water.renderTank(x + 44, y + 70, 0, 16, 52);
        be.steam.renderTank(x + 80, y + 70, 0, 16, 52);
        int hh = be.maxHeat <= 0 ? 0 : (int) ((long) be.heat * 52 / be.maxHeat);
        guiGraphics.fill(x + 116, y + 70 - hh, x + 132, y + 70, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        HeatBoilerBlockEntity be = this.getMenu().be;
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 18, 16, 52);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52,
                Component.literal(be.heat + " / " + be.maxHeat + " TU"));
    }
}
