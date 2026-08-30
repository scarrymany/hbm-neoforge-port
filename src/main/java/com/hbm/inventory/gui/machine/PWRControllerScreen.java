package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.PWRControllerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUIPWR}) as a plain panel - no {@code assets/hbm/textures/**}
 * tree exists in this port yet (see {@code GuiInfoContainer}'s own no-texture-yet rationale, shared
 * by every Phase 2 machine Screen so far). Shows the two fuel slots, both coolant tank gauges,
 * core/hull heat readouts, flux, and the stepped rod-level buttons ({@link PWRControllerMenu}'s own
 * javadoc explains why buttons replace CE's free-drag slider) - vanilla
 * {@link Button}-&gt;{@code MultiPlayerGameMode#handleInventoryButtonClick}-&gt;
 * {@link PWRControllerMenu#clickMenuButton}, the same plumbing
 * {@code MachineCombustionEngineScreen} already established for an identical slider-replacement
 * problem.
 */
public class PWRControllerScreen extends GuiInfoContainer<PWRControllerMenu> {

    public PWRControllerScreen(PWRControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int containerId = this.getMenu().containerId;
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, PWRControllerMenu.BUTTON_ROD_DOWN)
        ).bounds(leftPos + 8, topPos + 17, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, PWRControllerMenu.BUTTON_ROD_UP)
        ).bounds(leftPos + 30, topPos + 17, 20, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        this.getMenu().be.tanks[0].renderTank(x + 130, y + 71, 0, 16, 54);
        this.getMenu().be.tanks[1].renderTank(x + 152, y + 71, 0, 16, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 40, 100, 10, Component.literal("Rods: " + (int) be.rodLevel + "% / target " + (int) be.rodTarget + "%"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 52, 100, 10, Component.literal("Core heat: " + be.coreHeat + " / " + be.coreHeatCapacity));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 64, 100, 10, Component.literal("Hull heat: " + be.hullHeat));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 76, 100, 10, Component.literal("Flux: " + (int) be.flux));
    }
}
