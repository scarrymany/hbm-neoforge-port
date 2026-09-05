package com.hbm.inventory.gui.machine.reprocess;

import com.hbm.inventory.container.machine.reprocess.PurexMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Auto-recognition GUI (no CE recipe dropdown). Tanks + power + progress. */
public class PurexScreen extends GuiInfoContainer<PurexMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_purex.png");

    public PurexScreen(PurexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 216;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        for (int i = 0; i < 3; i++) {
            be.inputTanks[i].renderTank(x + 8 + i * 20, y + 140, 0, 16, 54);
        }
        be.outputTank.renderTank(x + 160, y + 140, 0, 16, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 20, 200, 10,
                Component.literal(be.isProcessing ? "Processing: " + be.getActiveRecipeName() : "Idle"),
                Component.literal("Progress: " + be.getProgressScaled(100) + "%"));
    }
}
