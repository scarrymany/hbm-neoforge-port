package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.TeleLinkerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineTeleLinker}. */
public class TeleLinkerScreen extends GuiInfoContainer<TeleLinkerMenu> {

    public TeleLinkerScreen(TeleLinkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
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
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 35, 16, 16,
                Component.literal("The first slot will copy the turret chip's"),
                Component.literal("UUIDs and add them to the second slot."));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 35, 16, 16,
                Component.literal("The third slot will clear the"),
                Component.literal("turret chip's UUID list."));
    }
}
