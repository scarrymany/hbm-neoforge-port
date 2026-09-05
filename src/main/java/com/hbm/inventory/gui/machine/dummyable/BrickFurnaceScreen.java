package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBrickFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.BrickFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIFurnaceBrick} on existing {@code gui_furnace_brick.png} 176×166.
 * Burn 62,{@code 54+12-b} from 176,{@code 12-b}; progress 85,34 from 176,14.
 * Invented {@code fill()} bars removed. Labels {@code 0xffffff} Exact CE {@code :29-30}.
 */
public class BrickFurnaceScreen extends GuiInfoContainer<BrickFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_furnace_brick.png");

    public BrickFurnaceScreen(BrickFurnaceMenu menu, Inventory inventory, Component title) {
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

        MachineBrickFurnaceBlockEntity be = this.getMenu().be;
        // CE GUIFurnaceBrick.java:42-46
        if (be.burnTime > 0) {
            int b = be.burnTime * 13 / Math.max(be.maxBurnTime, 1);
            guiGraphics.blit(TEXTURE, x + 62, y + 54 + 12 - b, 176, 12 - b, 14, b + 1);
            int p = be.progress * 24 / MachineBrickFurnaceBlockEntity.MAX_PROGRESS;
            guiGraphics.blit(TEXTURE, x + 85, y + 34, 176, 14, p + 1, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :29-30 — white labels
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xffffff, false);
    }
}
