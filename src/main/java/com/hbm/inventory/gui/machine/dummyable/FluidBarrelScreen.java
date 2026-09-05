package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FluidBarrelBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FluidBarrelMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code GUIBarrel}: {@code gui_barrel.png} 176×166. Mode 151,35. Tank 71,69 34×52. */
public class FluidBarrelScreen extends GuiInfoContainer<FluidBarrelMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_barrel.png");

    public FluidBarrelScreen(FluidBarrelMenu menu, Inventory inventory, Component title) {
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
        FluidBarrelBlockEntity be = this.getMenu().be;
        // CE GUIBarrel.java:65
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
        // CE GUIBarrel.java:50
        if (isHovered(mouseX, mouseY, 151, 35, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, FluidBarrelMenu.BUTTON_CYCLE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
