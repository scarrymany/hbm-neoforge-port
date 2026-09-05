package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachinePressBlockEntity;
import com.hbm.inventory.container.machine.dummyable.PressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachinePress} stamp/progress layout, painted panel. */
public class PressScreen extends GuiInfoContainer<PressMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_press.png");

    public PressScreen(PressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachinePressBlockEntity be = this.getMenu().be;
        int p = be.progress * 16 / MachinePressBlockEntity.MAX_PROGRESS;
        if (p > 0) guiGraphics.blit(TEXTURE, x + 80, y + 35, 176, 0, 16, p);
        int s = be.speed * 14 / MachinePressBlockEntity.MAX_SPEED;
        if (s > 0) guiGraphics.blit(TEXTURE, x + 25, y + 16, 192, 0, 14, s);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachinePressBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 35, 16, 16,
                Component.literal("Progress: " + (be.progress * 100 / MachinePressBlockEntity.MAX_PROGRESS) + "%"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 25, topPos + 16, 14, 14,
                Component.literal("Speed: " + be.speed),
                Component.literal("Burn: " + be.burnTime));
    }
}
