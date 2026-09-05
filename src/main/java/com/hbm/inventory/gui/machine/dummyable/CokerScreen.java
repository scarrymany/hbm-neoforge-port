package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCokerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CokerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Exact CE {@code GUIMachineCoker} on existing {@code gui_coker.png} 176×204.
 * Progress 61,46 from 176,0 (53×5); heat 61,55 from 176,5 (52×5); tanks 35/125,70.
 * Invented {@code fill()} bars removed.
 */
public class CokerScreen extends GuiInfoContainer<CokerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_coker.png");

    public CokerScreen(CokerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineCokerBlockEntity be = this.getMenu().be;
        // CE GUIMachineCoker.java:55-62
        int p = be.getProgressScaled(53);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 61, y + 46, 176, 0, p, 5);
        }
        int h = be.getHeatScaled(52);
        if (h > 0) {
            guiGraphics.blit(TEXTURE, x + 61, y + 55, 176, 5, h, 5);
        }
        be.input.renderTank(x + 35, y + 70, 0, 16, 52);
        be.output.renderTank(x + 125, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :43 — title 0xC7C1A3
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 0xC7C1A3, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineCokerBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 18, 16, 52);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 60, topPos + 45, 54, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.progress) + " / "
                        + String.format(Locale.US, "%,d", MachineCokerBlockEntity.PROCESS_TIME) + "TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 60, topPos + 54, 54, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.heat) + " / "
                        + String.format(Locale.US, "%,d", MachineCokerBlockEntity.MAX_HEAT) + "TU"));
    }
}
