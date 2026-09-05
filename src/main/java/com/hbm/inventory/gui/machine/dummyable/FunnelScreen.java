package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFunnelBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FunnelMenu;
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
 * Exact CE {@code GUIFunnel} on existing {@code gui_funnel.png} 176×168.
 * Mode icon 159,73 from 176,{@code mode*10}; click 159,73 {@code {toggle}}.
 * Invented Button widget removed.
 */
public class FunnelScreen extends GuiInfoContainer<FunnelMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_funnel.png");

    public FunnelScreen(FunnelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        // Exact CE GUIFunnel.java:69
        MachineFunnelBlockEntity be = this.getMenu().be;
        guiGraphics.blit(TEXTURE, x + 159, y + 73, 176, be.mode * 10, 10, 10);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineFunnelBlockEntity be = this.getMenu().be;
        String mode = be.mode == MachineFunnelBlockEntity.MODE_3x3 ? "3x3 only"
                : be.mode == MachineFunnelBlockEntity.MODE_2x2 ? "2x2 only" : "3x3 then 2x2";
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 159, topPos + 73, 10, 10, mouseX, mouseY,
                Component.literal("Mode: " + mode));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 159, 73, 10, 10)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
