package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceSteelBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FurnaceSteelMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Exact CE {@code GUIFurnaceSteel} on existing {@code gui_furnace_steel.png} 176×166.
 * Heat 152,{@code 67-h} from 176,{@code 76-h}; lanes 54,18+i×18 / bonus 54,27+i×18; wasOn 16,16+i×18.
 * Invented vertical {@code fill()} bars + 186px canvas removed.
 */
public class FurnaceSteelScreen extends GuiInfoContainer<FurnaceSteelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_furnace_steel.png");

    public FurnaceSteelScreen(FurnaceSteelMenu menu, Inventory inventory, Component title) {
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

        FurnaceSteelBlockEntity be = this.getMenu().be;
        // CE GUIFurnaceSteel.java:53-63
        int h = be.heat * 48 / FurnaceSteelBlockEntity.MAX_HEAT;
        if (h > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 67 - h, 176, 76 - h, 7, h);
        }
        for (int i = 0; i < 3; i++) {
            int p = be.progress[i] * 69 / FurnaceSteelBlockEntity.PROCESS_TIME;
            if (p > 0) {
                guiGraphics.blit(TEXTURE, x + 54, y + 18 + 18 * i, 176, 18, p, 5);
            }
            int b = be.bonus[i] * 69 / 100;
            if (b > 0) {
                guiGraphics.blit(TEXTURE, x + 54, y + 27 + 18 * i, 176, 23, b, 5);
            }
            if (be.wasOn) {
                guiGraphics.blit(TEXTURE, x + 16, y + 16 + 18 * i, 176, 0, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :42 — title centered
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        FurnaceSteelBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 3; i++) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 17 + 18 * i, 70, 7, mouseX, mouseY,
                    Component.literal(String.format(Locale.US, "%,d", be.progress[i]) + " / "
                            + String.format(Locale.US, "%,d", FurnaceSteelBlockEntity.PROCESS_TIME) + "TU"));
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 26 + 18 * i, 70, 7, mouseX, mouseY,
                    Component.literal("Bonus: " + be.bonus[i] + "%"));
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 151, topPos + 18, 9, 50, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.heat) + " / "
                        + String.format(Locale.US, "%,d", FurnaceSteelBlockEntity.MAX_HEAT) + "TU"));
    }
}
