package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineHydrotreaterBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HydrotreaterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineHydrotreater} 176×238 — 4 tanks + power + catalyst. */
public class HydrotreaterScreen extends GuiInfoContainer<HydrotreaterMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_hydrotreater.png");

public HydrotreaterScreen(HydrotreaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineHydrotreaterBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 17, y + 70 - ph, x + 33, y + 70, 0xFFFFCC00);
        be.oil.renderTank(x + 35, y + 70, 0, 16, 52);
        be.hydrogen.renderTank(x + 53, y + 70, 0, 16, 52);
        be.out1.renderTank(x + 125, y + 70, 0, 16, 52);
        be.out2.renderTank(x + 143, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineHydrotreaterBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 17, 18, 16, 52, be.getPower(), be.getMaxPower());
        be.oil.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 18, 16, 52);
        be.hydrogen.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 18, 16, 52);
        be.out1.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 18, 16, 52);
        be.out2.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52);
    }
}
