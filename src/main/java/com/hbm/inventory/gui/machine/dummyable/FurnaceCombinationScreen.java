package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceCombinationBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FurnaceCombinationMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Exact CE {@code GUIFurnaceCombo} on existing {@code gui_furnace_combination.png} 176×186.
 * Progress 45,37 from 176,0; heat 45,46 from 176,5; tank 118,70.
 * Invented {@code fill()} bars removed.
 */
public class FurnaceCombinationScreen extends GuiInfoContainer<FurnaceCombinationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_furnace_combination.png");

    public FurnaceCombinationScreen(FurnaceCombinationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        FurnaceCombinationBlockEntity be = this.getMenu().be;
        // CE GUIFurnaceCombo.java:57-63
        int p = be.progress * 38 / FurnaceCombinationBlockEntity.PROCESS_TIME;
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 45, y + 37, 176, 0, p, 5);
        }
        int h = be.heat * 37 / FurnaceCombinationBlockEntity.MAX_HEAT;
        if (h > 0) {
            guiGraphics.blit(TEXTURE, x + 45, y + 46, 176, 5, h, 5);
        }
        be.tank.renderTank(x + 118, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :46 — title centered
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        FurnaceCombinationBlockEntity be = this.getMenu().be;
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 118, topPos + 18, 16, 52);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 36, 39, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.progress) + " / "
                        + String.format(Locale.US, "%,d", FurnaceCombinationBlockEntity.PROCESS_TIME) + "TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 45, 39, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.heat) + " / "
                        + String.format(Locale.US, "%,d", FurnaceCombinationBlockEntity.MAX_HEAT) + "TU"));
    }
}
