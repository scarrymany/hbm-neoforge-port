package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineVacuumDistillBlockEntity;
import com.hbm.inventory.container.machine.dummyable.VacuumDistillMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineVacuumDistill} 176×238 — 5 tanks + power. */
public class VacuumDistillScreen extends GuiInfoContainer<VacuumDistillMenu> {

    public VacuumDistillScreen(VacuumDistillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineVacuumDistillBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 26, y + 70 - ph, x + 42, y + 70, 0xFFFFCC00);
        be.input.renderTank(x + 44, y + 70, 0, 16, 52);
        be.heavy.renderTank(x + 80, y + 70, 0, 16, 52);
        be.reformate.renderTank(x + 98, y + 70, 0, 16, 52);
        be.light.renderTank(x + 116, y + 70, 0, 16, 52);
        be.gas.renderTank(x + 134, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineVacuumDistillBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 26, 18, 16, 52, be.getPower(), be.getMaxPower());
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 18, 16, 52);
        be.heavy.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        be.reformate.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 18, 16, 52);
        be.light.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
        be.gas.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 18, 16, 52);
        if (be.isOn) {
            drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 60, topPos + 8, 50, 10, Component.literal("Refining"));
        }
    }
}
