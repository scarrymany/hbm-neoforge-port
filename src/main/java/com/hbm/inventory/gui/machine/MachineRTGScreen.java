package com.hbm.inventory.gui.machine;

import com.hbm.blockentity.machine.MachineRTGBlockEntity;
import com.hbm.inventory.container.machine.MachineRTGMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MachineRTGScreen extends GuiInfoContainer<MachineRTGMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/gui_rtg.png");

    public MachineRTGScreen(MachineRTGMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        if (be.heat > 0) {
            int h = be.heat * 52 / Math.max(1, MachineRTGBlockEntity.HEAT_MAX);
            guiGraphics.blit(TEXTURE, x + 134, y + 74 - h, 176, 52 - h, 16, h);
        }
        if (be.getPower() > 0) {
            int p = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
            guiGraphics.blit(TEXTURE, x + 152, y + 74 - p, 192, 52 - p, 16, p);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 69 - 52, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 69 - 52, 16, 52, mouseX + 8, mouseY - 8,
                Component.literal("RTG Heat " + be.heat + "/" + MachineRTGBlockEntity.HEAT_MAX),
                Component.literal("RTG Power " + (be.heat * 100) + "HE/s"));
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
