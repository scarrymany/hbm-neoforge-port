package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.RadioTelexBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadioTelexMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.FusionControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** CE {@code GuiScreenRadioTelex} — tx/rx buffers + snd/sve/rxprt/rxcls. */
public class RadioTelexScreen extends GuiInfoContainer<RadioTelexMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_telex.png");

    private final EditBox[] tx = new EditBox[5];
    private EditBox txChan;
    private EditBox rxChan;

    public RadioTelexScreen(RadioTelexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 215;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        RadioTelexBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 5; i++) {
            this.tx[i] = new EditBox(this.font, this.leftPos + 10, this.topPos + 18 + i * 12, 200, 11, Component.empty());
            this.tx[i].setMaxLength(RadioTelexBlockEntity.lineWidth);
            this.tx[i].setValue(be.txBuffer[i] == null ? "" : be.txBuffer[i]);
            this.tx[i].setBordered(false);
            this.tx[i].setTextColor(0x00FF00);
            this.addRenderableWidget(this.tx[i]);
        }
        this.txChan = new EditBox(this.font, this.leftPos + 10, this.topPos + 82, 80, 11, Component.empty());
        this.txChan.setValue(be.txChannel == null ? "" : be.txChannel);
        this.txChan.setBordered(false);
        this.txChan.setTextColor(0x00FF00);
        this.addRenderableWidget(this.txChan);
        this.rxChan = new EditBox(this.font, this.leftPos + 100, this.topPos + 82, 80, 11, Component.empty());
        this.rxChan.setValue(be.rxChannel == null ? "" : be.rxChannel);
        this.rxChan.setBordered(false);
        this.rxChan.setTextColor(0x00FF00);
        this.addRenderableWidget(this.rxChan);
        this.addRenderableWidget(Button.builder(Component.literal("SND"), b -> cmd("snd"))
                .bounds(this.leftPos + 190, this.topPos + 80, 28, 14).build());
        this.addRenderableWidget(Button.builder(Component.literal("SVE"), b -> cmd("sve"))
                .bounds(this.leftPos + 220, this.topPos + 80, 28, 14).build());
        this.addRenderableWidget(Button.builder(Component.literal("PRT"), b -> cmd("rxprt"))
                .bounds(this.leftPos + 190, this.topPos + 150, 28, 14).build());
        this.addRenderableWidget(Button.builder(Component.literal("CLS"), b -> cmd("rxcls"))
                .bounds(this.leftPos + 220, this.topPos + 150, 28, 14).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        RadioTelexBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 5; i++) {
            guiGraphics.drawString(this.font, be.rxBuffer[i] == null ? "" : be.rxBuffer[i], 10, 100 + i * 10, 0x55FF55, false);
        }
    }

    private void cmd(String cmd) {
        CompoundTag data = new CompoundTag();
        data.putString("cmd", cmd);
        if ("snd".equals(cmd)) {
            for (int j = 0; j < 5; j++) data.putString("tx" + j, this.tx[j].getValue());
        }
        if ("sve".equals(cmd)) {
            data.putString("txChan", this.txChan.getValue());
            data.putString("rxChan", this.rxChan.getValue());
        }
        PacketDistributor.sendToServer(new FusionControlPacket(this.getMenu().be.getBlockPos(), data));
    }
}
