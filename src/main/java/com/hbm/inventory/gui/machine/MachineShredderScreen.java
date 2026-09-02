package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineShredderMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUIMachineShredder}, canvas 176x233) as a plain panel - see
 * {@link com.hbm.inventory.gui.machine.MachineRTGScreen}'s javadoc for the no-texture-yet rationale
 * shared by every Phase 2 machine screen so far. Power bar at (guiLeft+8, guiTop+18) 16x88 vertical
 * fill, progress bar at (guiLeft+63, guiTop+89) scaled 0-34px - both hand-blit per
 * {@link GuiInfoContainer}'s own "progress bars are a convention, not a widget" javadoc.
 */
public class MachineShredderScreen extends GuiInfoContainer<MachineShredderMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_shredder.png");

    public MachineShredderScreen(MachineShredderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = this.imageHeight - 125;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Power bar - vertical fill, bottom-anchored.
        int power = (int) (88L * this.getMenu().be.getPower() / Math.max(1, this.getMenu().be.getMaxPower()));
        if (power > 0) guiGraphics.blit(TEXTURE, x + 8, y + 18 + (88 - power), 176, 88 - power, 16, power);

        // Progress bar.
        int progress = this.getMenu().be.getProgressScaled(34);
        if (progress > 0) guiGraphics.blit(TEXTURE, x + 63, y + 89, 192, 0, progress, 6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 88, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
    }
}
