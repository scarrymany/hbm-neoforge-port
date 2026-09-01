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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * CE {@code GUIElectrolyserMetal}. Texture is the unmodified CE png
 * ({@code textures/gui/processing/gui_electrolyser_metal.png}, 256×256).
 */
public class ElectrolyserMetalScreen extends GuiInfoContainer<ElectrolyserMetalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_electrolyser_metal.png");

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
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        ElectrolyserBlockEntity be = this.getMenu().be;
        blitMolten(guiGraphics, x + 58, y, be.leftStack, be.maxMaterial);
        blitMolten(guiGraphics, x + 96, y, be.rightStack, be.maxMaterial);
        guiGraphics.setColor(1F, 1F, 1F, 1F);

        int p = (int) (be.getMaxPower() > 0 ? be.getPower() * 89L / be.getMaxPower() : 0);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 186, y + 107 - p, 210, 89 - p, 16, p);
        }
        if (be.getPower() >= be.usageOre) {
            guiGraphics.blit(TEXTURE, x + 190, y + 4, 226, 25, 9, 12);
        }
        int max = Math.max(1, be.processOreTime);
        int o = be.progressOre * 26 / max;
        if (o > 0) {
            guiGraphics.blit(TEXTURE, x + 7, y + 71 - o, 226, 25 - o, 22, o);
        }

        be.tankAcid.renderTank(x + 36, y + 70, 0, 16, 52);
    }

    private void blitMolten(GuiGraphics guiGraphics, int x, int y, Mats.MaterialStack stack, int max) {
        if (stack == null || stack.material == null || stack.amount <= 0) return;
        int h = stack.amount * 42 / Math.max(1, max);
        if (h <= 0) return;
        int color = stack.material.moltenColor;
        guiGraphics.setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
        guiGraphics.blit(TEXTURE, x, y + 60 - h, 210, 131 - h, 34, h);
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
