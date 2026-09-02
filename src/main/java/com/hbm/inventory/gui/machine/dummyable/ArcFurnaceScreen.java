package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineArcFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ArcFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineArcFurnaceLarge} 176×256 — power + progress + liquid toggle. */
public class ArcFurnaceScreen extends GuiInfoContainer<ArcFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_arc_furnace.png");

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
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineArcFurnaceBlockEntity be = this.getMenu().be;
        int p = (int) (be.getPower() * 70 / Math.max(1, be.getMaxPower()));
        guiGraphics.blit(TEXTURE, x + 8, y + 106 - p, 176, 70 - p, 7, p);
        
        int o = (int) (be.progress * 70);
        guiGraphics.blit(TEXTURE, x + 17, y + 106 - o, 183, 70 - o, 7, o);
        
        if (be.liquidMode) guiGraphics.blit(TEXTURE, x + 151, y + 17, 190, 18, 18, 18);
        if (be.isProgressing) guiGraphics.blit(TEXTURE, x + 7, y + 17, 190, 0, 18, 18);
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
