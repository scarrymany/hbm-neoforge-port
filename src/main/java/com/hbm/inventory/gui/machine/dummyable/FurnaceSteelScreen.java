package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceSteelBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FurnaceSteelMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIFurnaceSteel} — heat + 3 progress lanes. */
public class FurnaceSteelScreen extends GuiInfoContainer<FurnaceSteelMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_furnace_steel.png");

public FurnaceSteelScreen(FurnaceSteelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        FurnaceSteelBlockEntity be = this.getMenu().be;
        int hh = be.heat * 52 / FurnaceSteelBlockEntity.MAX_HEAT;
        guiGraphics.fill(x + 143, y + 70 - hh, x + 159, y + 70, 0xFFFF4400);
        for (int i = 0; i < 3; i++) {
            int ph = be.progress[i] * 16 / FurnaceSteelBlockEntity.PROCESS_TIME;
            guiGraphics.fill(x + 35 + i * 18, y + 36, x + 49 + i * 18, y + 36 + ph, 0xFFFFFF55);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        FurnaceSteelBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52,
                Component.literal("Heat: " + be.heat + " / " + FurnaceSteelBlockEntity.MAX_HEAT));
    }
}
