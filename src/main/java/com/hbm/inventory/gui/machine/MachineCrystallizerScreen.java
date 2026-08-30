package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineCrystallizerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUICrystallizer}) as a plain panel - see {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. */
public class MachineCrystallizerScreen extends GuiInfoContainer<MachineCrystallizerMenu> {

    public MachineCrystallizerScreen(MachineCrystallizerMenu menu, Inventory inventory, Component title) {
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

        this.getMenu().be.tank.renderTank(x + 17, y + 72, 0, 16, 54);

        int progress = this.getMenu().be.getProgressScaled(18);
        guiGraphics.fill(x + 83, y + 45, x + 83 + progress, y + 51, 0xFF00A000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 54, 16, 16, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
        this.getMenu().be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 17, topPos + 72 - 54, 16, 54);
    }
}
