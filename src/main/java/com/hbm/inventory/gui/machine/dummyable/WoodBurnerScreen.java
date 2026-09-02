package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineWoodBurnerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.WoodBurnerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineWoodBurner} — tank + power + burn + on/liquid buttons. */
public class WoodBurnerScreen extends GuiInfoContainer<WoodBurnerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_wood_burner.png");

    public WoodBurnerScreen(WoodBurnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("On"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, WoodBurnerMenu.BUTTON_ON)
        ).bounds(leftPos + 52, topPos + 17, 28, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Oil"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, WoodBurnerMenu.BUTTON_LIQUID)
        ).bounds(leftPos + 52, topPos + 37, 28, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineWoodBurnerBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 143, y + 52 - ph, x + 159, y + 52, 0xFFFFCC00);
        be.tank.renderTank(x + 80, y + 52, 0, 16, 52);
        int bh = be.maxBurnTime <= 0 ? 0 : be.burnTime * 14 / be.maxBurnTime;
        if (bh > 0) guiGraphics.blit(TEXTURE, x + 26, y + 38 + (14 - bh), 176, 14 - bh, 14, bh);
        if (be.isOn) guiGraphics.fill(x + 52, y + 17, x + 80, y + 21, 0xFF44CC44);
        if (be.liquidBurn) guiGraphics.fill(x + 52, y + 37, x + 80, y + 41, 0xFF4488FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineWoodBurnerBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 143, 0, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 38, 14, 14,
                Component.literal("Burn: " + be.burnTime + " / " + be.maxBurnTime),
                Component.literal(be.powerGen + " HE/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 17, 28, 16,
                Component.literal(be.isOn ? "ON" : "OFF"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 37, 28, 16,
                Component.literal(be.liquidBurn ? "FLUID" : "SOLID"));
    }
}
