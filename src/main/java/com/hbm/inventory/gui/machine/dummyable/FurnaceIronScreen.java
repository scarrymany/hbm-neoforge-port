package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceIronBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FurnaceIronMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIFurnaceIron} — burn + progress. */
public class FurnaceIronScreen extends GuiInfoContainer<FurnaceIronMenu> {

    public FurnaceIronScreen(FurnaceIronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        FurnaceIronBlockEntity be = this.getMenu().be;
        int bh = be.maxBurnTime <= 0 ? 0 : be.burnTime * 14 / be.maxBurnTime;
        guiGraphics.fill(x + 62, y + 54 + (14 - bh), x + 76, y + 68, 0xFFFF6622);
        int ph = be.processingTime <= 0 ? 0 : be.progress * 24 / be.processingTime;
        guiGraphics.fill(x + 88, y + 36, x + 88 + ph, y + 50, 0xFFFFFF55);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        FurnaceIronBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 54, 14, 14,
                Component.literal("Burn: " + be.burnTime + " / " + be.maxBurnTime));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 88, topPos + 36, 24, 14,
                Component.literal("Progress: " + be.progress + " / " + be.processingTime));
    }
}
