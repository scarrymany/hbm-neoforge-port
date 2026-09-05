package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineReactorBreedingMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineReactorBreeding} on existing {@code gui_breeder.png} 176×166.
 * Progress 53,32 / flux 88,21 / info panel −16,16.
 */
public class MachineReactorBreedingScreen extends GuiInfoContainer<MachineReactorBreedingMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_breeder.png");

    public MachineReactorBreedingScreen(MachineReactorBreedingMenu menu, Inventory inventory, Component title) {
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

        // CE GUIMachineReactorBreeding.java:64-67
        int i = this.getMenu().be.getProgressScaled(70);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 53, y + 32, 176, 0, i, 20);
        }
        drawInfoPanel(guiGraphics, x - 16, y + 16, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :40-44
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        String flux = Integer.toString(this.getMenu().be.flux);
        int fluxX = 88 - this.font.width(flux) / 2;
        guiGraphics.drawString(this.font, flux, fluxX, 21, 0x08FF00, false);

        // CE :30-35 (keep CE wording)
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 16, topPos + 16, 16, 16,
                leftPos - 8, topPos + 32,
                Component.literal("The reactor has to recieve"),
                Component.literal("neutron flux from adjacent"),
                Component.literal("research reactors to breed."));
    }
}
