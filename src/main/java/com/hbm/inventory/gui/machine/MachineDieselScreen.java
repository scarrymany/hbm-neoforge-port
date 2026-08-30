package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineDieselMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUIMachineDiesel}) as a plain panel - see
 * {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. One vanilla {@link Button}
 * toggles {@code isOn} via {@link MachineDieselMenu#clickMenuButton}; the fuel tank renders through
 * {@link com.hbm.inventory.fluid.tank.FluidTankNTM#renderTank}.
 */
public class MachineDieselScreen extends GuiInfoContainer<MachineDieselMenu> {

    public MachineDieselScreen(MachineDieselMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("On/Off"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MachineDieselMenu.BUTTON_TOGGLE_ON)
        ).bounds(leftPos + 60, topPos + 17, 60, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        this.getMenu().be.tank.renderTank(x + 17, y + 71, 0, 16, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
        this.getMenu().be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 17, topPos + 71 - 54, 16, 54);
    }
}
