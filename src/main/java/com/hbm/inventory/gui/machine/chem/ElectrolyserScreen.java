package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.ElectrolyserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.ElectrolyserControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * CE {@code GUIElectrolyserFluid} layout (210×204). CE {@code gui_electrolyser_fluid.png} is not
 * in this tree — gray-box stand-in, same as the previous fluid screen.
 */
public class ElectrolyserScreen extends GuiInfoContainer<ElectrolyserMenu> {

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
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        guiGraphics.fill(x + 8, y + 82, x + 62, y + 94, 0xFF555577);

        var be = this.getMenu().be;
        be.tankIn.renderTank(x + 42, y + 70, 0, 16, 52);
        be.tankOut1.renderTank(x + 96, y + 70, 0, 16, 52);
        be.tankOut2.renderTank(x + 116, y + 70, 0, 16, 52);

        int max = Math.max(1, be.processFluidTime);
        int e = be.progressFluid * 41 / max;
        guiGraphics.fill(x + 62, y + 26, x + 74, y + 26 + e, 0xFF44AAEE);
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
        guiGraphics.drawString(this.font, "METAL", 16, 84, 0xFFFFFF, false);
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
