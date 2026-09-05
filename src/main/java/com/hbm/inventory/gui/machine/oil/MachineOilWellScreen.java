package com.hbm.inventory.gui.machine.oil;

import com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity;
import com.hbm.inventory.container.machine.oil.MachineOilWellMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Exact CE {@code GUIMachineOilWell} on existing {@code gui_well.png} 256×256, canvas 184×190.
 * Power 8,{@code 56-i} from atlas 184,{@code 34-i}; indicator 50,19 from {@code 184+(k-1)*14},34;
 * oil/gas 76/112,74 16×52; frack cover 48,44 when {@code tanks.size() < 3}; info 160,21 type 8.
 * Invented gray 184×202 + "Drilling..." text removed.
 */
public final class MachineOilWellScreen extends GuiInfoContainer<MachineOilWellMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_well.png");

    public MachineOilWellScreen(MachineOilWellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 184;
        this.imageHeight = 190;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        OilDrillBaseBlockEntity be = this.getMenu().be;
        // CE GUIMachineOilWell.java:66-85
        int i = (int) (be.getPower() * 34 / be.getMaxPower());
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 56 - i, 184, 34 - i, 16, i);
        }

        int k = be.indicator;
        if (k != 0) {
            guiGraphics.blit(TEXTURE, x + 50, y + 19, 184 + (k - 1) * 14, 34, 14, 14);
        }

        List<FluidTankNTM> tanks = be.tanks;
        if (tanks.size() < 3) {
            guiGraphics.blit(TEXTURE, x + 48, y + 44, 200, 0, 18, 34);
        }

        tanks.get(0).renderTank(x + 76, y + 74, 0, 16, 52);
        tanks.get(1).renderTank(x + 112, y + 74, 0, 16, 52);
        if (tanks.size() > 2) {
            tanks.get(2).renderTank(x + 54, y + 77, 0, 6, 32);
        }

        drawInfoPanel(guiGraphics, x + 160, y + 21, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :51-55 — title centered on x=126, inventory at 12, ySize-96+2
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 126 - this.font.width(name) / 2, 10, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        OilDrillBaseBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 22, 16, 34, be.getPower(), be.getMaxPower());
        be.tanks.get(0).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 76, topPos + 74 - 52, 16, 52);
        be.tanks.get(1).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 112, topPos + 74 - 52, 16, 52);
        if (be.tanks.size() >= 3) {
            be.tanks.get(2).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 54, topPos + 45, 6, 32);
        }
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 160, topPos + 21, 8, 8,
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.speed")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.power")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.afterburner")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.overdrive")));
    }
}
