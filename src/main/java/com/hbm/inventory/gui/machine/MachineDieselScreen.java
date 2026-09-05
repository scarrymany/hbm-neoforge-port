package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineDieselMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

/**
 * Exact CE {@code GUIMachineDiesel} on existing {@code gui_diesel.png} 176×203.
 * Power 141,{@code 69-i}; on 79,61; wasOn 89,42; click 89,61 {@code {turnOn}}.
 * Invented Button widget removed. Audio stay skipped.
 */
public class MachineDieselScreen extends GuiInfoContainer<MachineDieselMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/generators/gui_diesel.png");

    public MachineDieselScreen(MachineDieselMenu menu, Inventory inventory, Component title) {
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

        var be = this.getMenu().be;
        // CE GUIMachineDiesel.java:73-84
        if (be.getPower() > 0) {
            int p = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
            if (p > 0) {
                guiGraphics.blit(TEXTURE, x + 141, y + 69 - p, 176, 52 - p, 16, p);
            }
        }
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 79, y + 61, 192, 16, 35, 14);
        }
        if (be.wasOn) {
            guiGraphics.blit(TEXTURE, x + 89, y + 42, 192, 0, 16, 16);
        }
        drawInfoPanel(guiGraphics, x - 8, y + 36, 2);
        if (!be.hasAcceptableFuel()) {
            drawInfoPanel(guiGraphics, x - 8, y + 36 + 32, 6);
        }
        be.tank.renderTank(x + 35, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :63 — inventory only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        var be = this.getMenu().be;
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 69 - 52, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 141, topPos + 69 - 52, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 16, topPos + 36 + 16, 16, 16, leftPos - 8, topPos + 36 + 16,
                Arrays.stream(I18nUtil.resolveKeyArray("desc.guimachinediesel1")).map(Component::literal).toArray(Component[]::new));
        if (!be.hasAcceptableFuel()) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 8, topPos + 36 + 32, 16, 16, leftPos, topPos + 36 + 16 + 32,
                    Arrays.stream(I18nUtil.resolveKeyArray("desc.guimachinediesel2")).map(Component::literal).toArray(Component[]::new));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 89, 61, 16, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("turnOn", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
