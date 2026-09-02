package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineExposureChamberBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ExposureChamberMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineExposureChamber} 176×186 — power + progress + saved particles. */
public class ExposureChamberScreen extends GuiInfoContainer<ExposureChamberMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_exposure_chamber.png");

public ExposureChamberScreen(ExposureChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineExposureChamberBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 152, y + 52 - ph, x + 168, y + 52, 0xFFFFCC00);
        int p = be.processTime <= 0 ? 0 : be.progress * 54 / be.processTime;
        guiGraphics.fill(x + 26, y + 36, x + 26 + p, y + 44, be.isOn ? 0xFF66CCFF : 0xFF334466);
        int dots = be.savedParticles;
        for (int i = 0; i < 8; i++) {
            guiGraphics.fill(x + 26 + i * 6, y + 20, x + 30 + i * 6, y + 24, i < dots ? 0xFFAA66FF : 0xFF333333);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineExposureChamberBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 0, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 36, 54, 8,
                Component.literal(be.isOn ? "Exposing" : "Idle"),
                Component.literal(be.progress + "/" + be.processTime));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 20, 48, 4,
                Component.literal("Particles: " + be.savedParticles + "/" + MachineExposureChamberBlockEntity.MAX_PARTICLES));
    }
}
