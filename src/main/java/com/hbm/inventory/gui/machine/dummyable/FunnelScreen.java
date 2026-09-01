package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFunnelBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FunnelMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FunnelScreen extends GuiInfoContainer<FunnelMenu> {

    public FunnelScreen(FunnelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("M"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, FunnelMenu.BUTTON_CYCLE)
        ).bounds(leftPos + 8, topPos + 35, 18, 18).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineFunnelBlockEntity be = this.getMenu().be;
        String mode = switch (be.mode) {
            case MachineFunnelBlockEntity.MODE_3x3 -> "3x3";
            case MachineFunnelBlockEntity.MODE_2x2 -> "2x2";
            default -> "AUTO";
        };
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 35, 18, 18,
                Component.literal("Mode: " + mode));
    }
}
