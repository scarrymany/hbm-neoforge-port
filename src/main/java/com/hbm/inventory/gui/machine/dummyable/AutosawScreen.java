package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAutosawBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AutosawMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code MachineAutosaw} overlay — fuel + suspend. */
public class AutosawScreen extends GuiInfoContainer<AutosawMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_press.png");

public AutosawScreen(AutosawMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("On"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, AutosawMenu.BUTTON_SUSPEND)
        ).bounds(leftPos + 80, topPos + 17, 28, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineAutosawBlockEntity be = this.getMenu().be;
        be.tank.renderTank(x + 53, y + 70, 0, 16, 52);
        if (be.isOn) guiGraphics.fill(x + 80, y + 17, x + 108, y + 21, 0xFF44CC44);
        if (be.isSuspended) guiGraphics.fill(x + 80, y + 35, x + 108, y + 51, 0xFFAA2222);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineAutosawBlockEntity be = this.getMenu().be;
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 17, 28, 16,
                Component.literal(be.isSuspended ? "SUSPENDED" : (be.isOn ? "ON" : "OFF")));
    }
}
