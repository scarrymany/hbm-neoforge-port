package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachinePressBlockEntity;
import com.hbm.inventory.container.machine.dummyable.PressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachinePress} stamp/progress layout, painted panel. */
public class PressScreen extends GuiInfoContainer<PressMenu> {

    public PressScreen(PressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachinePressBlockEntity be = this.getMenu().be;
        int p = be.progress * 16 / MachinePressBlockEntity.MAX_PROGRESS;
        guiGraphics.fill(x + 80, y + 35, x + 96, y + 35 + p, 0xFF555555);
        int s = be.speed * 14 / MachinePressBlockEntity.MAX_SPEED;
        guiGraphics.fill(x + 25, y + 16 + (14 - s), x + 39, y + 30, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachinePressBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 35, 16, 16,
                Component.literal("Progress: " + (be.progress * 100 / MachinePressBlockEntity.MAX_PROGRESS) + "%"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 25, topPos + 16, 14, 14,
                Component.literal("Speed: " + be.speed),
                Component.literal("Burn: " + be.burnTime));
    }
}
