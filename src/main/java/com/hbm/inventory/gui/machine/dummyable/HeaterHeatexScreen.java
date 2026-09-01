package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterHeatexBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HeaterHeatexMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIHeaterHeatex} 176×204 — hot/cold tanks + heat. */
public class HeaterHeatexScreen extends GuiInfoContainer<HeaterHeatexMenu> {

    public HeaterHeatexScreen(HeaterHeatexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, HeaterHeatexMenu.BUTTON_COOL_UP)
        ).bounds(leftPos + 8, topPos + 18, 16, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, HeaterHeatexMenu.BUTTON_COOL_DOWN)
        ).bounds(leftPos + 8, topPos + 36, 16, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("D+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, HeaterHeatexMenu.BUTTON_DELAY_UP)
        ).bounds(leftPos + 152, topPos + 18, 16, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("D-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, HeaterHeatexMenu.BUTTON_DELAY_DOWN)
        ).bounds(leftPos + 152, topPos + 36, 16, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        HeaterHeatexBlockEntity be = this.getMenu().be;
        be.hot.renderTank(x + 44, y + 88, 0, 16, 52);
        be.cold.renderTank(x + 116, y + 88, 0, 16, 52);
        int hh = Math.min(52, be.heatEnergy / 200);
        guiGraphics.fill(x + 80, y + 70 - hh, x + 96, y + 70, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        HeaterHeatexBlockEntity be = this.getMenu().be;
        be.hot.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 36, 16, 52);
        be.cold.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 36, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52,
                Component.literal(be.heatEnergy + " TU"),
                Component.literal(be.amountToCool + " mB / " + be.tickDelay + "t"));
    }
}
