package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOvenBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HeaterOvenMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exact CE {@code GUIFirebox} on existing {@code gui_heating_oven.png} 176×168 (oven texture, same layout).
 * Title color {@code 0xffffff} Exact CE {@code GUIFirebox.java:58}. Invented vertical fill removed.
 */
public class HeaterOvenScreen extends GuiInfoContainer<HeaterOvenMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_heating_oven.png");

    public HeaterOvenScreen(HeaterOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        HeaterOvenBlockEntity be = this.getMenu().be;
        // CE GUIFirebox.java:71-78
        int i = be.heatEnergy * 69 / be.getMaxHeat();
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 81, y + 28, 176, 0, i, 5);
        }
        int j = be.burnTime * 70 / Math.max(be.maxBurnTime, 1);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 81, y + 37, 176, 5, j, 5);
        }
        if (be.wasOn) {
            guiGraphics.blit(TEXTURE, x + 25, y + 26, 176, 10, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :58-61 — oven title white
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        HeaterOvenBlockEntity be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 27, 71, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.heatEnergy) + " / "
                        + String.format(Locale.US, "%,d", be.getMaxHeat()) + "TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 36, 71, 7, mouseX, mouseY,
                Component.literal(be.burnHeat + "TU/t"),
                Component.literal((be.burnTime / 20) + "s"));

        if (this.menu.getCarried().isEmpty()) {
            List<String> bonuses = be.getModule().getDesc();
            if (!bonuses.isEmpty()) {
                for (int s = 0; s < 2; s++) {
                    Slot slot = this.menu.getSlot(s);
                    if (!slot.hasItem() && isHovered(mouseX, mouseY, slot.x, slot.y, 16, 16)) {
                        List<Component> lines = new ArrayList<>(bonuses.size());
                        for (String line : bonuses) {
                            lines.add(Component.literal(line));
                        }
                        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }
}
