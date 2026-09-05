package com.hbm.inventory.gui.machine;

import com.hbm.blockentity.machine.MachineTurbineBlockEntity;
import com.hbm.inventory.container.machine.MachineTurbineMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineTurbine} on existing {@code gui_turbine.png} 176×168.
 * Steam icon 99,18 / power 123,69-i / tanks 62,69 + 134,69. Slots already CE.
 */
public class MachineTurbineScreen extends GuiInfoContainer<MachineTurbineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_turbine.png");

    public MachineTurbineScreen(MachineTurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineTurbineBlockEntity be = this.getMenu().be;
        // CE GUIMachineTurbine.java:60-66
        var in = be.tanks[0].getTankType();
        if (in == Fluids.STEAM) {
            guiGraphics.blit(TEXTURE, x + 99, y + 18, 183, 0, 14, 14);
        } else if (in == Fluids.HOTSTEAM) {
            guiGraphics.blit(TEXTURE, x + 99, y + 18, 183, 14, 14, 14);
        } else if (in == Fluids.SUPERHOTSTEAM) {
            guiGraphics.blit(TEXTURE, x + 99, y + 18, 183, 28, 14, 14);
        } else if (in == Fluids.ULTRAHOTSTEAM) {
            guiGraphics.blit(TEXTURE, x + 99, y + 18, 183, 42, 14, 14);
        }

        int i = (int) be.getPowerScaled(34);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 123, y + 69 - i, 176, 34 - i, 7, i);
        }

        if (be.tanks[1].getTankType() == Fluids.NONE) {
            drawInfoPanel(guiGraphics, x - 16, y + 36 + 32, 6);
        }

        be.tanks[0].renderTank(x + 62, y + 69, 0, 16, 52);
        be.tanks[1].renderTank(x + 134, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :45-49
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineTurbineBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 123, topPos + 69 - 34, 7, 34,
                be.getPower(), MachineTurbineBlockEntity.MAX_POWER);
        be.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 69 - 52, 16, 52);
        be.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 69 - 52, 16, 52);
    }
}
