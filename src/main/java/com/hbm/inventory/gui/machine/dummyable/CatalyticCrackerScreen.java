package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCatalyticCrackerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CatalyticCrackerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Real 5-tank inspect GUI (CE used overlay only). */
public class CatalyticCrackerScreen extends GuiInfoContainer<CatalyticCrackerMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_fluid.png");

public CatalyticCrackerScreen(CatalyticCrackerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineCatalyticCrackerBlockEntity be = this.getMenu().be;
        be.oil.renderTank(x + 26, y + 70, 0, 16, 52);
        be.steam.renderTank(x + 48, y + 70, 0, 16, 52);
        be.left.renderTank(x + 92, y + 70, 0, 16, 52);
        be.right.renderTank(x + 114, y + 70, 0, 16, 52);
        be.spent.renderTank(x + 136, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineCatalyticCrackerBlockEntity be = this.getMenu().be;
        be.oil.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 48, topPos + 18, 16, 52);
        be.left.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 92, topPos + 18, 16, 52);
        be.right.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 114, topPos + 18, 16, 52);
        be.spent.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 136, topPos + 18, 16, 52);
    }
}
