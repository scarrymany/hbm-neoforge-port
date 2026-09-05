package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBigAssTankBlockEntity;
import com.hbm.inventory.container.machine.dummyable.BigAssTankMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIBarrel} reused by BigAssTank — {@code gui_barrel.png} 176×166. */
public class BigAssTankScreen extends GuiInfoContainer<BigAssTankMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_barrel.png");

    public BigAssTankScreen(BigAssTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        MachineBigAssTankBlockEntity be = this.getMenu().be;
        guiGraphics.blit(TEXTURE, x + 151, y + 34, 176, be.mode * 18, 18, 18);
        be.tank.renderTank(x + 71, y + 69, 0, 34, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        this.getMenu().be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 71, topPos + 17, 34, 52);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 151, 35, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, BigAssTankMenu.BUTTON_CYCLE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
