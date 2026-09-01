package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineMicrowaveBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MicrowaveMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MicrowaveScreen extends GuiInfoContainer<MicrowaveMenu> {

    public MicrowaveScreen(MicrowaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MicrowaveMenu.BUTTON_UP)
        ).bounds(leftPos + 133, topPos + 17, 16, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MicrowaveMenu.BUTTON_DOWN)
        ).bounds(leftPos + 133, topPos + 35, 16, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineMicrowaveBlockEntity be = this.getMenu().be;
        long max = Math.max(1L, be.getMaxPower());
        int ph = (int) (be.power * 52L / max);
        guiGraphics.fill(x + 26, y + 70 - ph, x + 42, y + 70, 0xFF44CCFF);
        int prog = be.time * 24 / Math.max(1, MachineMicrowaveBlockEntity.MAX_TIME);
        guiGraphics.fill(x + 79, y + 18, x + 79 + prog, y + 32, 0xFFFFFF55);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineMicrowaveBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52,
                Component.literal(be.power + " / " + be.getMaxPower() + " HE"),
                Component.literal(MachineMicrowaveBlockEntity.CONSUMPTION + " HE/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 18, 24, 14,
                Component.literal(be.time + " / " + MachineMicrowaveBlockEntity.MAX_TIME));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 133, topPos + 17, 16, 34,
                Component.literal("speed " + be.speed));
    }
}
