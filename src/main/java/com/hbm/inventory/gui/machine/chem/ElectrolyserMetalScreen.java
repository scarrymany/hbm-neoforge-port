package com.hbm.inventory.gui.machine.chem;

import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
import com.hbm.inventory.container.machine.chem.ElectrolyserMetalMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.packet.toserver.ElectrolyserControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * CE {@code GUIElectrolyserMetal} layout (210×204). CE {@code gui_electrolyser_metal.png} is not
 * in this tree — gray-box stand-in matching the fluid screen.
 */
public class ElectrolyserMetalScreen extends GuiInfoContainer<ElectrolyserMetalMenu> {

    public ElectrolyserMetalScreen(ElectrolyserMetalMenu menu, Inventory inventory, Component title) {
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
        guiGraphics.fill(x + 8, y + 82, x + 62, y + 94, 0xFF775555);

        ElectrolyserBlockEntity be = this.getMenu().be;
        be.tankAcid.renderTank(x + 36, y + 70, 0, 16, 52);
        fillStack(guiGraphics, x + 58, y + 18, be.leftStack, be.maxMaterial);
        fillStack(guiGraphics, x + 96, y + 18, be.rightStack, be.maxMaterial);

        int max = Math.max(1, be.processOreTime);
        int o = be.progressOre * 26 / max;
        guiGraphics.fill(x + 7, y + 71 - o, x + 29, y + 71, 0xFFCC8844);
    }

    private static void fillStack(GuiGraphics guiGraphics, int x, int y, Mats.MaterialStack stack, int max) {
        guiGraphics.fill(x, y, x + 34, y + 42, 0xFF222222);
        if (stack == null || stack.material == null || stack.amount <= 0) return;
        int h = stack.amount * 42 / Math.max(1, max);
        int color = 0xFF000000 | stack.material.moltenColor;
        guiGraphics.fill(x, y + 42 - h, x + 34, y + 42, color);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        ElectrolyserBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 186, topPos + 18, 16, 89, be.getPower(), be.getMaxPower());
        be.tankAcid.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 36, topPos + 18, 16, 52);
        stackTip(guiGraphics, mouseX, mouseY, leftPos + 58, topPos + 18, be.leftStack);
        stackTip(guiGraphics, mouseX, mouseY, leftPos + 96, topPos + 18, be.rightStack);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 82, 54, 12, Component.literal("Fluid"));
        guiGraphics.drawString(this.font, "FLUID", 16, 84, 0xFFFFFF, false);
    }

    private void stackTip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, Mats.MaterialStack stack) {
        if (stack == null || stack.material == null) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, x, y, 34, 42, mouseX, mouseY,
                    Component.literal("Empty").withStyle(ChatFormatting.RED));
            return;
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, x, y, 34, 42, mouseX, mouseY,
                Component.empty()
                        .append(stack.material.getName())
                        .append(": ")
                        .append(Mats.formatAmount(stack.amount, Screen.hasShiftDown()))
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 8, 82, 54, 12)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("sgf", true);
            PacketDistributor.sendToServer(new ElectrolyserControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
