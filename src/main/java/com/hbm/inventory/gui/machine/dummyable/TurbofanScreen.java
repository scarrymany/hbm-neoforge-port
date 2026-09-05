package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineTurbofanBlockEntity;
import com.hbm.inventory.container.machine.dummyable.TurbofanMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineTurbofan} on existing {@code gui_turbofan.png} 176×203.
 * Power 143,{@code 69-i} from 192,{@code 52-i}; afterburner 98,44; tank 35,69 34×52.
 * Invented {@code fill()} bars + SPINNING/IDLE tooltips removed.
 * Blood {@code GUIElements.renderGauge} stay skipped (blood tank not in this port).
 */
public class TurbofanScreen extends GuiInfoContainer<TurbofanMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_turbofan.png");

    public TurbofanScreen(TurbofanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineTurbofanBlockEntity be = this.getMenu().be;
        // CE GUIMachineTurbofan.java:52-59
        if (be.power > 0) {
            int i = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
            if (i > 0) {
                guiGraphics.blit(TEXTURE, x + 152 - 9, y + 69 - i, 176 + 16, 52 - i, 16, i);
            }
        }
        if (be.afterburner > 0) {
            int a = Math.min(be.afterburner, 6);
            guiGraphics.blit(TEXTURE, x + 98, y + 44, 176, (a - 1) * 16, 16, 16);
        }
        be.tank.renderTank(x + 35, y + 69, 0, 34, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :41 — title at x=43
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 43 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineTurbofanBlockEntity be = this.getMenu().be;
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 17, 34, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 17, 16, 52, be.getPower(), be.getMaxPower());
    }
}
