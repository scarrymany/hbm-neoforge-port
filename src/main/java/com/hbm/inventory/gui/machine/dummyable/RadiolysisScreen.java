package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRadiolysisBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadiolysisMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIRadiolysis} — 3 tanks + RTG heat + power. Wide for 2×5 pellet grid. */
public class RadiolysisScreen extends GuiInfoContainer<RadiolysisMenu> {

    public RadiolysisScreen(RadiolysisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineRadiolysisBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 34 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 8, y + 51 - ph, x + 24, y + 51, 0xFFFFCC00);
        int hh = Math.min(52, be.heat * 52 / Math.max(1, 400));
        guiGraphics.fill(x + 56, y + 70 - hh, x + 64, y + 70, 0xFFFF4400);
        be.input.renderTank(x + 70, y + 70, 0, 16, 52);
        be.out1.renderTank(x + 106, y + 70, 0, 16, 52);
        be.out2.renderTank(x + 124, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRadiolysisBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 17, 16, 34, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 56, topPos + 18, 8, 52,
                Component.literal("Heat: " + be.heat));
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 70, topPos + 18, 16, 52);
        be.out1.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 106, topPos + 18, 16, 52);
        be.out2.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 124, topPos + 18, 16, 52);
    }
}
