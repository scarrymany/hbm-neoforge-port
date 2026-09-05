package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineWoodBurnerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.WoodBurnerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIMachineWoodBurner} on existing {@code gui_wood_burner_alt.png} 176×186.
 * Liquid overlay 16,17+79,17; on 53,17; power 143,{@code 52-p}; burn 17,{@code 70-b}; tank 80,70.
 * Invented Button widgets and power {@code fill()} removed.
 */
public class WoodBurnerScreen extends GuiInfoContainer<WoodBurnerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_wood_burner_alt.png");

    public WoodBurnerScreen(WoodBurnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineWoodBurnerBlockEntity be = this.getMenu().be;
        // CE GUIMachineWoodBurner.java:98-115
        if (be.liquidBurn) {
            guiGraphics.blit(TEXTURE, x + 16, y + 17, 176, 52, 60, 54);
            guiGraphics.blit(TEXTURE, x + 79, y + 17, 176, 106, 36, 54);
        }
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 53, y + 17, 196, 0, 16, 15);
        }
        int p = (int) (be.getPower() * 34 / Math.max(1L, be.getMaxPower()));
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 143, y + 52 - p, 176, 52 - p, 16, p);
        }
        if (be.maxBurnTime > 0 && !be.liquidBurn) {
            int b = be.burnTime * 52 / be.maxBurnTime;
            if (b > 0) {
                guiGraphics.blit(TEXTURE, x + 17, y + 70 - b, 192, 52 - b, 4, b);
            }
        }
        if (be.liquidBurn) {
            be.tank.renderTank(x + 80, y + 70, 0, 16, 52);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :87 — title centered on x=70, white
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(name) / 2, 6, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineWoodBurnerBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 34, be.getPower(), be.getMaxPower());

        if (this.menu.getCarried().isEmpty()) {
            Slot slot = this.menu.getSlot(0);
            if (!slot.hasItem() && isHovered(mouseX, mouseY, slot.x, slot.y, 16, 16)) {
                List<String> bonuses = MachineWoodBurnerBlockEntity.burnModule.getDesc();
                if (!bonuses.isEmpty()) {
                    List<Component> lines = new ArrayList<>(bonuses.size());
                    for (String line : bonuses) {
                        lines.add(Component.literal(line));
                    }
                    guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                }
            }
        }
        if (be.liquidBurn) {
            be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        }
        if (!be.liquidBurn) {
            drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 16, topPos + 17, 8, 54,
                    Component.literal((be.burnTime / 20) + "s"));
        }
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 17, 16, 15,
                Component.literal((be.isOn ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF")));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMachineWoodBurner.java:68-80
        if (isHovered(mouseX, mouseY, 53, 17, 16, 15)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", false);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        if (isHovered(mouseX, mouseY, 46, 37, 30, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("switch", false);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
