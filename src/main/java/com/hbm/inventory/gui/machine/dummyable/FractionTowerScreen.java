package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFractionTowerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FractionTowerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tank inspect GUI for the fraction tower (CE used chat; this is the real menu). */
public class FractionTowerScreen extends GuiInfoContainer<FractionTowerMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_fluid.png");

public FractionTowerScreen(FractionTowerMenu menu, Inventory inventory, Component title) {
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

        MachineFractionTowerBlockEntity be = this.getMenu().be;
        be.input.renderTank(x + 44, y + 70, 0, 16, 52);
        be.left.renderTank(x + 80, y + 70, 0, 16, 52);
        be.right.renderTank(x + 116, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineFractionTowerBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 18, 16, 52);
        be.left.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        be.right.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
    }
}
