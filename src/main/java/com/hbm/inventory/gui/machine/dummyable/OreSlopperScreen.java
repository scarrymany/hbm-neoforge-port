package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineOreSlopperBlockEntity;
import com.hbm.inventory.container.machine.dummyable.OreSlopperMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIOreSlopper} — water/slop tanks + power + progress. */
public class OreSlopperScreen extends GuiInfoContainer<OreSlopperMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_ore_slopper.png");

    public OreSlopperScreen(OreSlopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineOreSlopperBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        if (ph > 0) guiGraphics.blit(TEXTURE, x + 8, y + 70 - ph, 176, 52 - ph, 16, ph);
        int p = (int) (be.progress * 35);
        if (p > 0) guiGraphics.fill(x + 89, y + 29, x + 89 + p, y + 41, be.processing ? 0xFF44AAFF : 0xFF335577);
        be.water.renderTank(x + 44, y + 70, 0, 16, 52);
        be.slop.renderTank(x + 107, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineOreSlopperBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 52, be.getPower(), be.getMaxPower());
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 18, 16, 52);
        be.slop.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 107, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 89, topPos + 29, 35, 12,
                Component.literal(be.processing ? "Slopping" : "Idle"),
                Component.literal(String.format("%.0f%%", be.progress * 100)));
    }
}
