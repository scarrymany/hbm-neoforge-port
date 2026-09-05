package com.hbm.inventory.gui.machine.workshop;

import com.hbm.inventory.container.machine.workshop.SolderingMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Exact CE {@code GUIMachineSolderingStation} on existing {@code gui_soldering_station.png} 176×204.
 * Collision 5,66 + info 78,67. Inventory label only. IUpgradeInfoProvider stay skipped —
 * tooltip uses CE {@code getUpgradeInfo} SPEED/POWER/OVERDRIVE 3 from {@code VALID_UPGRADES}.
 */
public class SolderingScreen extends GuiInfoContainer<SolderingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_soldering_station.png");

    public SolderingScreen(SolderingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int p = (int) (be.getPower() * 52 / Math.max(be.getMaxPower(), 1));
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 70 - p, 176, 52 - p, 16, p);
        }
        int prog = be.getProgressScaled(33);
        if (prog > 0) {
            guiGraphics.blit(TEXTURE, x + 72, y + 28, 192, 0, prog, 14);
        }
        if (be.getPower() >= be.consumption) {
            guiGraphics.blit(TEXTURE, x + 156, y + 4, 176, 52, 9, 12);
        }
        // Exact CE GUIMachineSolderingStation.java:117
        drawInfoPanel(guiGraphics, x + 78, y + 67, 8);
        // Exact CE GUIMachineSolderingStation.java:118 — horizontal tank at 35,79.
        be.tank.renderTank(x + 35, y + 79, 0, 34, 16, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :94-95 — inventory only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 63, 34, 16);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 78, topPos + 67, 8, 8, leftPos + 78, topPos + 67,
                Component.literal(I18nUtil.resolveKey("upgrade.gui.title")),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.speed", 3)),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.power", 3)),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.overdrive", 3)));
        // Exact CE GUIMachineSolderingStation.java:65-76
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 5, topPos + 66, 10, 10, mouseX, mouseY, List.of(
                Component.literal("Recipe Collision Prevention: ")
                        .append(Component.literal(be.collisionPrevention ? "ON" : "OFF")
                                .withStyle(be.collisionPrevention ? ChatFormatting.GREEN : ChatFormatting.RED)),
                Component.literal("Prevents no-fluid recipes from being processed"),
                Component.literal("when fluid is present.")));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 5, 66, 10, 10)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("collision", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
