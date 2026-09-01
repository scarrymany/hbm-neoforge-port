package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCompressorBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CompressorMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUICompressor} 176×204 — tanks, PU buttons, progress, power. */
public class CompressorScreen extends GuiInfoContainer<CompressorMenu> {

    public CompressorScreen(CompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i <= 5; i++) {
            final int pu = i;
            this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(i)), b ->
                    this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, pu)
            ).bounds(leftPos + 43 + i * 11, topPos + 46, 10, 12).build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineCompressorBlockEntity be = this.getMenu().be;
        int p = be.getProgressScaled(55);
        guiGraphics.fill(x + 42, y + 26, x + 42 + p, y + 34, be.isOn ? 0xFF44AAFF : 0xFF335577);
        be.input.renderTank(x + 17, y + 70, 0, 16, 52);
        be.output.renderTank(x + 107, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineCompressorBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 18, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 42, topPos + 26, 55, 8,
                Component.literal(be.isOn ? "Compressing" : "Idle"),
                Component.literal("PU in: " + be.input.getPressure() + "  PU out: " + be.output.getPressure()),
                Component.literal(be.getProgressScaled(100) + "%"));
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 17, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 107, topPos + 18, 16, 52);
    }
}
