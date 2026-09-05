package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineExposureChamberBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ExposureChamberMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineExposureChamber} on existing {@code gui_exposure_chamber.png} 176×186.
 * Progress 36,39 from 192,0; particles 26,{@code 52-c} from 192,{@code 26-c}; power 152,{@code 52-e};
 * pip 156,4. Invented {@code fill()} bars removed.
 */
public class ExposureChamberScreen extends GuiInfoContainer<ExposureChamberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_exposure_chamber.png");

    public ExposureChamberScreen(ExposureChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineExposureChamberBlockEntity be = this.getMenu().be;
        // CE GUIMachineExposureChamber.java:50-61
        int p = be.progress * 42 / (be.processTime + 1);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 36, y + 39, 192, 0, p, 10);
        }
        int c = be.savedParticles * 16 / MachineExposureChamberBlockEntity.MAX_PARTICLES;
        if (c > 0) {
            guiGraphics.blit(TEXTURE, x + 26, y + 52 - c, 192, 26 - c, 9, c);
        }
        int e = (int) (be.getPower() * 34 / Math.max(1L, be.getMaxPower()));
        if (e > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 52 - e, 176, 34 - e, 16, e);
        }
        if (be.consumption <= be.getPower()) {
            guiGraphics.blit(TEXTURE, x + 156, y + 4, 176, 34, 9, 12);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :39 — title centered on x=70
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineExposureChamberBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 34, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 36, 9, 16, mouseX, mouseY,
                Component.literal(be.savedParticles + " / " + MachineExposureChamberBlockEntity.MAX_PARTICLES));
    }
}
