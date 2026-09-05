package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.OilburnerMenu;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Exact CE {@code GUIOilburner} on existing {@code gui_oilburner.png} 176×203.
 * Heat 116,{@code 69-i} from 194; on 70,54; flame 79,34; tank 44,69. Toggle click 80,54.
 * Invented Button widgets and heat {@code fill()} removed.
 */
public class OilburnerScreen extends GuiInfoContainer<OilburnerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_oilburner.png");

    public OilburnerScreen(OilburnerMenu menu, Inventory inventory, Component title) {
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

        HeaterOilburnerBlockEntity be = this.getMenu().be;
        // CE GUIOilburner.java:76-87
        int i = be.heatEnergy * 52 / HeaterOilburnerBlockEntity.MAX_HEAT;
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 116, y + 69 - i, 194, 52 - i, 16, i);
        }
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 70, y + 54, 210, 0, 35, 14);
            if (be.tank.getFill() > 0 && be.tank.getTankType().hasTrait(FT_Flammable.class)) {
                guiGraphics.blit(TEXTURE, x + 79, y + 34, 176, 0, 18, 18);
            }
        }
        be.tank.renderTank(x + 44, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        HeaterOilburnerBlockEntity be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 17, 16, 52, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", Math.min(be.heatEnergy, HeaterOilburnerBlockEntity.MAX_HEAT))
                        + " / " + String.format(Locale.US, "%,d", HeaterOilburnerBlockEntity.MAX_HEAT) + " TU"));
        if (be.tank.getTankType().hasTrait(FT_Flammable.class)) {
            FT_Flammable trait = be.tank.getTankType().getTrait(FT_Flammable.class);
            int tu = (int) (trait.getHeatEnergy() / 1000) * be.setting;
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 34, 18, 18, mouseX, mouseY,
                    Component.literal(be.setting + " mB/t"),
                    Component.literal(String.format(Locale.US, "%,d", tu) + " TU/t"));
        }
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 17, 16, 52);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIOilburner.java:52-57
        if (isHovered(mouseX, mouseY, 80, 54, 16, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
