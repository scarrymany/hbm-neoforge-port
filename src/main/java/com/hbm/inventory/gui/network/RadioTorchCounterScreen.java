package com.hbm.inventory.gui.network;

import com.hbm.blockentity.network.RadioTorchCounterBlockEntity;
import com.hbm.inventory.container.network.RadioTorchCounterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.FusionControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class RadioTorchCounterScreen extends GuiInfoContainer<RadioTorchCounterMenu> {

    private final EditBox[] channels = new EditBox[3];

    public RadioTorchCounterScreen(RadioTorchCounterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        RadioTorchCounterBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 3; i++) {
            this.channels[i] = new EditBox(this.font, this.leftPos + 8 + i * 56, this.topPos + 18, 52, 12, Component.empty());
            this.channels[i].setValue(be.channels[i] == null ? "" : be.channels[i]);
            this.addRenderableWidget(this.channels[i]);
        }
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> push())
                .bounds(this.leftPos + 48, this.topPos + 58, 80, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A2A20);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF4A4A38);
    }

    private void push() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < 3; i++) {
            data.putString("channel" + i, this.channels[i].getValue());
        }
        PacketDistributor.sendToServer(new FusionControlPacket(this.getMenu().be.getBlockPos(), data));
    }
}
