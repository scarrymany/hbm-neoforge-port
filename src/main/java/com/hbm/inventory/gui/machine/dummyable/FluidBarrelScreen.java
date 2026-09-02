package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FluidBarrelBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FluidBarrelMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FluidBarrelScreen extends GuiInfoContainer<FluidBarrelMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_barrel.png");

    public FluidBarrelScreen(FluidBarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("M"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, FluidBarrelMenu.BUTTON_CYCLE)
        ).bounds(leftPos + 8, topPos + 53, 18, 18).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.getMenu().be.tank.renderTank(x + 80, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        FluidBarrelBlockEntity be = this.getMenu().be;
        this.getMenu().be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        String mode = switch (be.mode) {
            case FluidBarrelBlockEntity.MODE_IN -> "IN";
            case FluidBarrelBlockEntity.MODE_OUT -> "OUT";
            case FluidBarrelBlockEntity.MODE_NONE -> "OFF";
            default -> "BOTH";
        };
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 53, 18, 18,
                Component.literal("Mode: " + mode));
    }
}
