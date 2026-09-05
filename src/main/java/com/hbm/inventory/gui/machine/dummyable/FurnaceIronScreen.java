package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceIronBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FurnaceIronMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIFurnaceIron} on existing {@code gui_furnace_iron.png} 176×166.
 * Progress 53,36 from 176,18; burn 53,45 from 176,23; canSmelt 70,16 from 176,0.
 * Invented {@code fill()} bars + 168px canvas removed.
 */
public class FurnaceIronScreen extends GuiInfoContainer<FurnaceIronMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_furnace_iron.png");

    public FurnaceIronScreen(FurnaceIronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        FurnaceIronBlockEntity be = this.getMenu().be;
        // CE GUIFurnaceIron.java:49-56
        int i = be.progress * 70 / Math.max(be.processingTime, 1);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 53, y + 36, 176, 18, i, 5);
        }
        int j = be.burnTime * 70 / Math.max(be.maxBurnTime, 1);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 53, y + 45, 176, 23, j, 5);
        }
        if (be.canSmelt()) {
            guiGraphics.blit(TEXTURE, x + 70, y + 16, 176, 0, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :38 — title centered
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        FurnaceIronBlockEntity be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 35, 71, 7, mouseX, mouseY,
                Component.literal((be.progress * 100 / Math.max(be.processingTime, 1)) + "%"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 44, 71, 7, mouseX, mouseY,
                Component.literal((be.burnTime / 20) + "s"));
    }
}
