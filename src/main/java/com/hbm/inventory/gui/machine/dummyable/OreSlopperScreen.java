package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineOreSlopperBlockEntity;
import com.hbm.inventory.container.machine.dummyable.OreSlopperMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIOreSlopper} on existing {@code gui_ore_slopper.png} 176×204.
 * Progress 62,{@code 52-i} from 176,{@code 34-i}; power 8,{@code 70-j} from 176,{@code 86-j};
 * pip 12,4; tanks 26/116. Invented progress {@code fill()} + wrong tank columns removed.
 */
public class OreSlopperScreen extends GuiInfoContainer<OreSlopperMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_ore_slopper.png");

    public OreSlopperScreen(OreSlopperMenu menu, Inventory inventory, Component title) {
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

        MachineOreSlopperBlockEntity be = this.getMenu().be;
        // CE GUIOreSlopper.java:52-62
        int i = (int) (be.progress * 35);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 62, y + 52 - i, 176, 34 - i, 34, i);
        }
        int j = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 70 - j, 176, 86 - j, 16, j);
        }
        if (be.getPower() >= be.consumption) {
            guiGraphics.blit(TEXTURE, x + 12, y + 4, 202, 34, 9, 12);
        }
        be.water.renderTank(x + 26, y + 70, 0, 16, 52);
        be.slop.renderTank(x + 116, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :40 — title centered with -9
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2 - 9, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineOreSlopperBlockEntity be = this.getMenu().be;
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 34, 52);
        be.slop.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
    }
}
