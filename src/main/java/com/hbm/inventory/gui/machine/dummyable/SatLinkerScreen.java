package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.inventory.container.machine.dummyable.SatLinkerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Arrays;

/** CE {@code GUIMachineSatLinker} — {@code gui_sat_linker.png} 176×186. */
public class SatLinkerScreen extends GuiInfoContainer<SatLinkerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_sat_linker.png");

    public SatLinkerScreen(SatLinkerMenu menu, Inventory inventory, Component title) {
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
        drawInfoPanel(guiGraphics, x + 12, y + 28, 2);
        drawInfoPanel(guiGraphics, x + 12, y + 44, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        Component[] chip = Arrays.stream(I18nUtil.resolveKeyArray("desc.gui.satlinker.chip"))
                .map(Component::literal).toArray(Component[]::new);
        Component[] random = Arrays.stream(I18nUtil.resolveKeyArray("desc.gui.satlinker.random"))
                .map(Component::literal).toArray(Component[]::new);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 12, topPos + 28, 16, 16,
                leftPos - 8, topPos + 52, chip);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 12, topPos + 44, 16, 16,
                leftPos - 8, topPos + 52, random);
    }
}
