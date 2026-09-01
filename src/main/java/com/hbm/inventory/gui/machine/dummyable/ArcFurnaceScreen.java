package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineArcFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ArcFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineArcFurnaceLarge} 176×256 — power + progress + liquid toggle. */
public class ArcFurnaceScreen extends GuiInfoContainer<ArcFurnaceMenu> {

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("Mode"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, ArcFurnaceMenu.BUTTON_LIQUID)
        ).bounds(leftPos + 8, topPos + 22, 40, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineArcFurnaceBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 8, y + 106 - ph, x + 24, y + 106, 0xFFFFCC00);
        int p = (int) (be.progress * 54);
        guiGraphics.fill(x + 62, y + 42, x + 62 + p, y + 50, be.isProgressing ? 0xFFFF4400 : 0xFF442200);
        if (be.liquidMode) guiGraphics.fill(x + 152, y + 22, x + 168, y + 30, 0xFF4488FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineArcFurnaceBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 54, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 42, 54, 8,
                Component.literal(be.isProgressing ? "Arcing" : "Idle"),
                Component.literal(String.format("%.0f%%", be.progress * 100)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 22, 16, 8,
                Component.literal(be.liquidMode ? "LIQUID" : "SOLID"));
        if (!be.liquids.isEmpty()) {
            StringBuilder sb = new StringBuilder("Melt: ");
            for (Mats.MaterialStack s : be.liquids) {
                sb.append(s.material.getRegistryName()).append(' ').append(s.amount).append(' ');
            }
            drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 8, 160, 10, Component.literal(sb.toString()));
        }
    }
}
