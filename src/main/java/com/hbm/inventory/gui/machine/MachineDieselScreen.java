package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineDieselMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MachineDieselScreen extends GuiInfoContainer<MachineDieselMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/generators/gui_diesel.png");

    public MachineDieselScreen(MachineDieselMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("On/Off"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MachineDieselMenu.BUTTON_TOGGLE_ON)
        ).bounds(leftPos + 79, topPos + 61, 35, 14).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        if (be.getPower() > 0) {
            int p = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
            guiGraphics.blit(TEXTURE, x + 141, y + 69 - p, 176, 52 - p, 16, p);
        }

        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 79, y + 61, 192, 16, 35, 14);
        }
        if (be.wasOn) {
            guiGraphics.blit(TEXTURE, x + 89, y + 42, 192, 0, 16, 16);
        }

        be.tank.renderTank(x + 35, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 141, topPos + 69 - 52, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 69 - 52, 16, 52);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
