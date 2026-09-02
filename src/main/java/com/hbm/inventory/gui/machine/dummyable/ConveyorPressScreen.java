package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineConveyorPressBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ConveyorPressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Stamp slot + power/press readout. CE has no GUI; this exposes the live 1-slot inventory. */
public class ConveyorPressScreen extends GuiInfoContainer<ConveyorPressMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_press.png");

    public ConveyorPressScreen(ConveyorPressMenu menu, Inventory inventory, Component title) {
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

        MachineConveyorPressBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 152, y + 69 - ph, x + 168, y + 69, 0xFFFFCC00);
        int p = (int) (be.press * 16);
        guiGraphics.fill(x + 80, y + 17, x + 80 + 16, y + 17 + p, 0xFF555555);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineConveyorPressBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 17, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 17, 16, 16,
                Component.literal("Press: " + (int) (be.press * 100) + "%"));
    }
}
