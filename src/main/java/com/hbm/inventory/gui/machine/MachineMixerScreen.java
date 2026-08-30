package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineMixerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUIMixer}) as a plain panel - see {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. */
public class MachineMixerScreen extends GuiInfoContainer<MachineMixerMenu> {

    public MachineMixerScreen(MachineMixerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 191;
        this.inventoryLabelY = this.imageHeight - 91;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        this.getMenu().be.tanks.get(0).renderTank(x + 63, y + 77, 0, 16, 54);
        this.getMenu().be.tanks.get(1).renderTank(x + 81, y + 77, 0, 16, 54);
        this.getMenu().be.tanks.get(2).renderTank(x + 137, y + 77, 0, 16, 54);

        int progress = this.getMenu().be.getProgressScaled(18);
        guiGraphics.fill(x + 99, y + 60, x + 99 + progress, y + 66, 0xFF00A000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 23, 60, 16, 16, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
        this.getMenu().be.tanks.get(0).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 63, topPos + 77 - 54, 16, 54);
        this.getMenu().be.tanks.get(1).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 81, topPos + 77 - 54, 16, 54);
        this.getMenu().be.tanks.get(2).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 137, topPos + 77 - 54, 16, 54);
    }
}
