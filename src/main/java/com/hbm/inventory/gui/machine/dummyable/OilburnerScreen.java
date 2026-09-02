package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.OilburnerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIOilburner} — tank + heat + on/setting. */
public class OilburnerScreen extends GuiInfoContainer<OilburnerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_oilburner.png");

    public OilburnerScreen(OilburnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("On"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, OilburnerMenu.BUTTON_ON)
        ).bounds(leftPos + 80, topPos + 17, 28, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, OilburnerMenu.BUTTON_UP)
        ).bounds(leftPos + 112, topPos + 17, 16, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, OilburnerMenu.BUTTON_DOWN)
        ).bounds(leftPos + 112, topPos + 35, 16, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        HeaterOilburnerBlockEntity be = this.getMenu().be;
        be.tank.renderTank(x + 53, y + 70, 0, 16, 52);
        int hh = be.heatEnergy * 52 / HeaterOilburnerBlockEntity.MAX_HEAT;
        guiGraphics.fill(x + 143, y + 70 - hh, x + 159, y + 70, 0xFFFF4400);
        if (be.isOn) guiGraphics.fill(x + 80, y + 17, x + 108, y + 21, 0xFF44CC44);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        HeaterOilburnerBlockEntity be = this.getMenu().be;
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52,
                Component.literal(be.heatEnergy + " TU"),
                Component.literal(be.setting + " mB/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 17, 28, 16,
                Component.literal(be.isOn ? "ON" : "OFF"));
    }
}
