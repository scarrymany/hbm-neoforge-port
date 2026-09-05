package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceRtgBlockEntity;
import com.hbm.inventory.container.machine.dummyable.DiFurnaceRtgMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIDiFurnaceRTG} on existing {@code gui_rtg_difurnace.png} 176×166.
 * Heat pip 58,36 from 176,31; progress 101,35 from 176,14; info -15,36 type 3.
 * Invented vertical {@code fill()} bars removed.
 */
public class DiFurnaceRtgScreen extends GuiInfoContainer<DiFurnaceRtgMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_rtg_difurnace.png");

    public DiFurnaceRtgScreen(DiFurnaceRtgMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineDiFurnaceRtgBlockEntity be = this.getMenu().be;
        // CE GUIDiFurnaceRTG.java:57-62
        if (be.hasPower()) {
            guiGraphics.blit(TEXTURE, x + 58, y + 36, 176, 31, 18, 16);
        }
        int p = be.getDiFurnaceProgressScaled(24);
        guiGraphics.blit(TEXTURE, x + 101, y + 35, 176, 14, p + 1, 17);
        drawInfoPanel(guiGraphics, x - 15, y + 36, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineDiFurnaceRtgBlockEntity be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 15, topPos + 36, 16, 16, leftPos - 8, topPos + 36 + 16,
                toComponents(I18nUtil.resolveKeyArray("desc.gui.rtgBFurnace.desc", MachineDiFurnaceRtgBlockEntity.MAX_HEAT)));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 58, topPos + 36, 18, 16, mouseX, mouseY,
                toComponents(I18nUtil.resolveKeyArray("desc.gui.rtg.heat", be.heat)));
    }

    private static List<Component> toComponents(String[] lines) {
        List<Component> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(Component.literal(line));
        }
        return out;
    }
}
