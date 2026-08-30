package com.hbm.inventory.gui.bomb;

import com.hbm.inventory.container.bomb.NukeBalefireMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Flat-panel GUI for {@link NukeBalefireMenu} - arm button plus the +/-60s timer-step buttons (see
 * that Menu's own javadoc for why CE's free-drag slider becomes stepped buttons here), same
 * {@code Button}-&gt;{@code handleInventoryButtonClick}-&gt;{@code clickMenuButton} plumbing as
 * {@code PWRControllerScreen}.
 */
public class NukeBalefireScreen extends GuiInfoContainer<NukeBalefireMenu> {

    public NukeBalefireScreen(NukeBalefireMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 174;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int containerId = this.getMenu().containerId;
        this.addRenderableWidget(Button.builder(Component.literal("Arm"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, NukeBalefireMenu.BUTTON_START)
        ).bounds(leftPos + 8, topPos + 44, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-60s"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, NukeBalefireMenu.BUTTON_TIMER_DOWN)
        ).bounds(leftPos + 52, topPos + 44, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+60s"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, NukeBalefireMenu.BUTTON_TIMER_UP)
        ).bounds(leftPos + 96, topPos + 44, 40, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 68, 140, 10,
                Component.literal("Timer: " + be.getMinutes() + ":" + be.getSeconds() + (be.started ? " (armed)" : "")));
    }
}
