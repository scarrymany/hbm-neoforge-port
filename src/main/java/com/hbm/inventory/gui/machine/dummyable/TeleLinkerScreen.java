package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.TeleLinkerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineTeleLinker}. */
public class TeleLinkerScreen extends GuiInfoContainer<TeleLinkerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_sat_linker.png");

    public TeleLinkerScreen(TeleLinkerMenu menu, Inventory inventory, Component title) {
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
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 35, 16, 16,
                Component.literal("The first slot will copy the turret chip's"),
                Component.literal("UUIDs and add them to the second slot."));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 35, 16, 16,
                Component.literal("The third slot will clear the"),
                Component.literal("turret chip's UUID list."));
    }
}
