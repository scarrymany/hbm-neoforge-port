package com.hbm.inventory.gui.network;

import com.hbm.blockentity.network.RadioTorchBaseBlockEntity;
import com.hbm.inventory.container.network.RadioTorchMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.FusionControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** CE {@code GUIScreenRadioTorch} — channel + polling. Mapping grid left as NBT. */
public class RadioTorchScreen extends GuiInfoContainer<RadioTorchMenu> {

    private EditBox channel;

    public RadioTorchScreen(RadioTorchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        RadioTorchBaseBlockEntity be = this.getMenu().be;
        this.channel = new EditBox(this.font, this.leftPos + 28, this.topPos + 20, 120, 12, Component.empty());
        this.channel.setValue(be.channel == null ? "" : be.channel);
        this.addRenderableWidget(this.channel);
        this.addRenderableWidget(Button.builder(
                Component.literal(be.polling ? "Polling ON" : "Polling OFF"),
                b -> {
                    be.polling = !be.polling;
                    b.setMessage(Component.literal(be.polling ? "Polling ON" : "Polling OFF"));
                    push();
                }).bounds(this.leftPos + 28, this.topPos + 38, 120, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> push())
                .bounds(this.leftPos + 28, this.topPos + 58, 120, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A2A20);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF4A4A38);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, "Signal " + this.getMenu().be.lastState, 8, 78, 0xFF5555, false);
    }

    private void push() {
        CompoundTag data = new CompoundTag();
        data.putString("channel", this.channel.getValue());
        data.putBoolean("isPolling", this.getMenu().be.polling);
        PacketDistributor.sendToServer(new FusionControlPacket(this.getMenu().be.getBlockPos(), data));
    }
}
