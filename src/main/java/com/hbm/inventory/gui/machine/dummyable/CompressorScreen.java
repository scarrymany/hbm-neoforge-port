package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCompressorBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CompressorMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Exact CE {@code GUICompressor} on existing {@code gui_compressor.png} 176×204.
 * Power pip 156,4; PU knob 43+p*11,46; progress 42,26 from 192,0; power 152,{@code 70-j}.
 * Invented Button widgets and progress {@code fill()} removed.
 */
public class CompressorScreen extends GuiInfoContainer<CompressorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_compressor.png");

    public CompressorScreen(CompressorMenu menu, Inventory inventory, Component title) {
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

        MachineCompressorBlockEntity be = this.getMenu().be;
        // CE GUICompressor.java:75-88
        if (be.getPower() >= be.powerRequirement) {
            guiGraphics.blit(TEXTURE, x + 156, y + 4, 176, 52, 9, 12);
        }
        // CE blit h=124 is a typo vs click/tooltip 8×14 (:40/:50)
        guiGraphics.blit(TEXTURE, x + 43 + be.input.getPressure() * 11, y + 46, 193, 18, 8, 14);
        int i = be.getProgressScaled(55);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 42, y + 26, 192, 0, i, 17);
        }
        int j = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 70 - j, 176, 52 - j, 16, j);
        }
        be.input.renderTank(x + 17, y + 70, 0, 16, 52);
        be.output.renderTank(x + 107, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :64 — title centered on x=70, 0xC7C1A3
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(name) / 2, 6, 0xC7C1A3, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineCompressorBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 17, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 107, topPos + 18, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        for (int pu = 0; pu < 5; pu++) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 43 + pu * 11, topPos + 46, 8, 14, mouseX, mouseY,
                    Component.literal(pu + " PU -> " + (pu + 1) + " PU"));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUICompressor.java:49-56
        for (int pu = 0; pu < 5; pu++) {
            if (isHovered(mouseX, mouseY, 43 + pu * 11, 46, 8, 14)) {
                click();
                CompoundTag data = new CompoundTag();
                data.putInt("compression", pu);
                PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
