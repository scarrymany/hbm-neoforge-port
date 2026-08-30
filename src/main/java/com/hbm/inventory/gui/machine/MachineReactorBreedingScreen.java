package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineReactorBreedingMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUIMachineReactorBreeding}) as a plain panel - see {@code GuiInfoContainer}'s own no-texture-yet rationale. */
public class MachineReactorBreedingScreen extends GuiInfoContainer<MachineReactorBreedingMenu> {

    public MachineReactorBreedingScreen(MachineReactorBreedingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        int progress = this.getMenu().be.getProgressScaled(24);
        guiGraphics.fill(x + 80, y + 50 - progress, x + 96, y + 50, 0xFF55AAFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 20, 160, 10, Component.literal("Flux: " + this.getMenu().be.flux));
    }
}
