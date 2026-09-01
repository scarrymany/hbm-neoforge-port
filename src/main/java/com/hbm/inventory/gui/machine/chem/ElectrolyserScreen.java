package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.ElectrolyserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.ElectrolyserControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * CE {@code GUIElectrolyserFluid}. Texture is the unmodified CE png
 * ({@code textures/gui/processing/gui_electrolyser_fluid.png}, 256×256).
 */
public class ElectrolyserScreen extends GuiInfoContainer<ElectrolyserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_electrolyser_fluid.png");

    public ElectrolyserScreen(ElectrolyserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 210;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = this.imageWidth / 2 - 16;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int p = (int) (be.getMaxPower() > 0 ? be.getPower() * 89L / be.getMaxPower() : 0);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 186, y + 107 - p, 210, 89 - p, 16, p);
        }
        if (be.getPower() >= be.usageFluid) {
            guiGraphics.blit(TEXTURE, x + 190, y + 4, 226, 40, 9, 12);
        }
        int max = Math.max(1, be.processFluidTime);
        int e = be.progressFluid * 41 / max;
        if (e > 0) {
            guiGraphics.blit(TEXTURE, x + 62, y + 26, 226, 0, 12, e);
        }

        be.tankIn.renderTank(x + 42, y + 70, 0, 16, 52);
        be.tankOut1.renderTank(x + 96, y + 70, 0, 16, 52);
        be.tankOut2.renderTank(x + 116, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 186, topPos + 18, 16, 89, be.getPower(), be.getMaxPower());
        be.tankIn.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 42, topPos + 18, 16, 52);
        be.tankOut1.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 96, topPos + 18, 16, 52);
        be.tankOut2.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 82, 54, 12, Component.literal("Metal"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 8, 82, 54, 12)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("sgm", true);
            PacketDistributor.sendToServer(new ElectrolyserControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
