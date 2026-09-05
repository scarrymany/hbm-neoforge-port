package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineMicrowaveBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MicrowaveMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMicrowave} on existing {@code gui_microwave.png} 176×168.
 * Power 8,51-i / progress 104,34 / speed 62,60-k. Buttons 43,25 / 43,43.
 */
public class MicrowaveScreen extends GuiInfoContainer<MicrowaveMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_microwave.png");

    public MicrowaveScreen(MicrowaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineMicrowaveBlockEntity be = this.getMenu().be;
        // CE GUIMicrowave.java:63-70
        int i = (int) be.getPowerScaled(34);
        guiGraphics.blit(TEXTURE, x + 8, y + 51 - i, 176, 34 - i, 16, i);
        int j = Math.min(be.getProgressScaled(23), 22);
        guiGraphics.blit(TEXTURE, x + 104, y + 34, 192, 0, j, 16);
        int k = be.getSpeedScaled(34);
        guiGraphics.blit(TEXTURE, x + 62, y + 60 - k, 214, 34 - k, 4, k);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineMicrowaveBlockEntity be = this.getMenu().be;
        // CE GUIMicrowave.java:33
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 17, 16, 34,
                be.power, MachineMicrowaveBlockEntity.MAX_POWER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMicrowave.java:40-45
        if (isHovered(mouseX, mouseY, 43, 25, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MicrowaveMenu.BUTTON_UP);
            return true;
        }
        if (isHovered(mouseX, mouseY, 43, 43, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MicrowaveMenu.BUTTON_DOWN);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
