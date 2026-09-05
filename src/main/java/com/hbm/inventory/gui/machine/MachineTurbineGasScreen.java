package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineTurbineGasMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Plain panel — {@code gui_turbinegas.png} is not in this tree (do not invent). Slot 1 fluid-ID
 * sits at Exact CE 36,17 so Start/Stop is at 8,38 to keep that slot clickable.
 * Four tanks (gas, lubricant, water, hot steam); four buttons drive {@link MachineTurbineGasMenu}.
 */
public class MachineTurbineGasScreen extends GuiInfoContainer<MachineTurbineGasMenu> {

    public MachineTurbineGasScreen(MachineTurbineGasMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 81;
    }

    @Override
    protected void init() {
        super.init();
        int containerId = this.getMenu().containerId;
        this.addRenderableWidget(Button.builder(Component.literal("Start/Stop"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineTurbineGasMenu.BUTTON_TOGGLE_RUN)
        ).bounds(leftPos + 8, topPos + 38, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Auto"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineTurbineGasMenu.BUTTON_TOGGLE_AUTO)
        ).bounds(leftPos + 80, topPos + 17, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineTurbineGasMenu.BUTTON_THROTTLE_DOWN)
        ).bounds(leftPos + 122, topPos + 17, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineTurbineGasMenu.BUTTON_THROTTLE_UP)
        ).bounds(leftPos + 144, topPos + 17, 20, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        var be = this.getMenu().be;
        be.tanks[0].renderTank(x + 8, y + 90, 0, 16, 36);
        be.tanks[1].renderTank(x + 34, y + 90, 0, 16, 36);
        be.tanks[2].renderTank(x + 60, y + 90, 0, 16, 36);
        be.tanks[3].renderTank(x + 86, y + 90, 0, 16, 36);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 42, 160, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 60, 160, 10,
                Component.literal("RPM: " + be.rpm + "  Temp: " + be.temp + "C  State: " + be.state));
        be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 90 - 36, 16, 36);
        be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 34, topPos + 90 - 36, 16, 36);
        be.tanks[2].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 60, topPos + 90 - 36, 16, 36);
        be.tanks[3].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 86, topPos + 90 - 36, 16, 36);
    }
}
