package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineGasFlareBlockEntity;
import com.hbm.inventory.container.machine.dummyable.GasFlareMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineGasFlare} — tank + valve/ignition buttons. */
public class GasFlareScreen extends GuiInfoContainer<GasFlareMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_flare_stack.png");

public GasFlareScreen(GasFlareMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("Valve"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, GasFlareMenu.BUTTON_VALVE)
        ).bounds(leftPos + 70, topPos + 17, 40, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Ignite"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, GasFlareMenu.BUTTON_BURN)
        ).bounds(leftPos + 70, topPos + 37, 40, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineGasFlareBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 143, y + 69 - ph, x + 159, y + 69, 0xFFFFCC00);
        be.tank.renderTank(x + 35, y + 69, 0, 16, 52);
        if (be.isOn) guiGraphics.fill(x + 116, y + 17, x + 132, y + 25, 0xFF44CC44);
        if (be.doesBurn) guiGraphics.fill(x + 116, y + 37, x + 132, y + 45, 0xFFFF6622);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineGasFlareBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 143, 17, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 17, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 17, 16, 8,
                Component.literal(be.isOn ? "Valve OPEN" : "Valve CLOSED"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 37, 16, 8,
                Component.literal(be.doesBurn ? "Ignition ON" : "Ignition OFF"));
    }
}
