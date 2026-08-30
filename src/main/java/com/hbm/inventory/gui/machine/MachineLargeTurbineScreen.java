package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineLargeTurbineMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUIMachineLargeTurbine}) as a plain panel - see
 * {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. Purely passive.
 */
public class MachineLargeTurbineScreen extends GuiInfoContainer<MachineLargeTurbineMenu> {

    public MachineLargeTurbineScreen(MachineLargeTurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 165;
        this.inventoryLabelY = this.imageHeight - 76;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        this.getMenu().be.tanks[0].renderTank(x + 8, y + 53, 0, 16, 36);
        this.getMenu().be.tanks[1].renderTank(x + 152, y + 53, 0, 16, 36);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
        this.getMenu().be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 53 - 36, 16, 36);
        this.getMenu().be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 53 - 36, 16, 36);
    }
}
