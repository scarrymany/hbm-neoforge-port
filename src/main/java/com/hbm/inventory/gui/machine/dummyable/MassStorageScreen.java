package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MassStorageBlockEntity;
import com.hbm.client.ClientScreens;
import com.hbm.inventory.container.machine.dummyable.MassStorageMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.MassStorageControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public class MassStorageScreen extends GuiInfoContainer<MassStorageMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/gui_test_storage.png");

    public MassStorageScreen(MassStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 221;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MassStorageBlockEntity be = this.getMenu().be;
        int cap = Math.max(1, be.getCapacity());
        int gauge = be.getStockpile() * 88 / cap;
        guiGraphics.blit(TEXTURE, x + 97, y + 105 - gauge, 176, 88 - gauge, 16, gauge);

        if (be.output) {
            guiGraphics.blit(TEXTURE, x + 80, y + 72, 192, 0, 14, 14);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        MassStorageBlockEntity be = this.getMenu().be;
        String percent = (((int) (be.getStockpile() * 1000D / (double) Math.max(1, be.getCapacity()))) / 10D) + "%";
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 96, topPos + 16, 18, 90, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.getStockpile()) + " / "
                        + String.format(Locale.US, "%,d", be.getCapacity())),
                Component.literal(percent));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 72, 14, 14,
                Component.literal("Click: Provide one"), Component.literal("Shift-click: Provide stack"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 72, 14, 14,
                Component.literal("Toggle output"));
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 62, 72, 14, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("provide", ClientScreens.hasShiftDown());
            PacketDistributor.sendToServer(new MassStorageControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        if (isHovered(mouseX, mouseY, 80, 72, 14, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", false);
            PacketDistributor.sendToServer(new MassStorageControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
