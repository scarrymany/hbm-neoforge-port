package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.StorageDrumBlockEntity;
import com.hbm.inventory.container.machine.dummyable.StorageDrumMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIStorageDrum} 176×237 — liquid/gas gauges. */
public class StorageDrumScreen extends GuiInfoContainer<StorageDrumMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_drum.png");

    public StorageDrumScreen(StorageDrumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 237;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        StorageDrumBlockEntity be = this.getMenu().be;
        be.liquid.renderTank(x + 16, y + 131, 0, 9, 108);
        be.gas.renderTank(x + 151, y + 131, 0, 9, 108);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        StorageDrumBlockEntity be = this.getMenu().be;
        be.liquid.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 16, topPos + 23, 9, 108);
        be.gas.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 151, topPos + 23, 9, 108);
    }
}
