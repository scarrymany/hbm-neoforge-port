package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.ElectrolyserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (fluid side only, see {@code ElectrolyserBlockEntity}'s javadoc) from CE's {@code GUIElectrolyserFluid}. */
public class ElectrolyserScreen extends GuiInfoContainer<ElectrolyserMenu> {

    public ElectrolyserScreen(ElectrolyserMenu menu, Inventory inventory, Component title) {
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

        var be = this.getMenu().be;
        be.tankIn.renderTank(x + 60, y + 90, 0, 16, 54);
        be.tankOut1.renderTank(x + 90, y + 90, 0, 16, 54);
        be.tankOut2.renderTank(x + 110, y + 90, 0, 16, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, be.getPower(), be.getMaxPower());
    }
}
