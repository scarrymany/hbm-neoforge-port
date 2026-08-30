package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineAssemblyMachineMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUIMachineAssemblyMachine}) as a plain panel - see {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. */
public class MachineAssemblyMachineScreen extends GuiInfoContainer<MachineAssemblyMachineMenu> {

    public MachineAssemblyMachineScreen(MachineAssemblyMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        int progress = this.getMenu().be.getProgressScaled(24);
        guiGraphics.fill(x + 66, y + 45, x + 66 + progress, y + 51, 0xFF00A000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 63, 18, 18, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
    }
}
