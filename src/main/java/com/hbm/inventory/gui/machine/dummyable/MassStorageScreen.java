package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MassStorageBlockEntity;
import com.hbm.client.ClientScreens;
import com.hbm.inventory.container.machine.dummyable.MassStorageMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.MassStorageControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/** CE {@code GUIMassStorage}: gauge + provide / toggle output. */
public class MassStorageScreen extends GuiInfoContainer<MassStorageMenu> {

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
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MassStorageBlockEntity be = this.getMenu().be;
        int cap = Math.max(1, be.getCapacity());
        int gauge = be.getStockpile() * 88 / cap;
        guiGraphics.fill(x + 97, y + 105 - gauge, x + 113, y + 105, 0xFF55AA55);

        if (be.output) {
            guiGraphics.fill(x + 80, y + 72, x + 94, y + 86, 0xFF44AA44);
        } else {
            guiGraphics.fill(x + 80, y + 72, x + 94, y + 86, 0xFF555555);
        }
        guiGraphics.fill(x + 62, y + 72, x + 76, y + 86, 0xFF888888);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
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
