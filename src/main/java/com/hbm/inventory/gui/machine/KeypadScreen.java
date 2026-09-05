package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.KeypadMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.KeypadServerPacket;
import com.hbm.util.Keypad;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Functional 12-button keypad. CE KeypadClient OBJ click is VFX — not this wave. */
public class KeypadScreen extends GuiInfoContainer<KeypadMenu> {

    public KeypadScreen(KeypadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 140;
        this.imageHeight = 166;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "R", "0", "E"};
        for (int i = 0; i < 12; i++) {
            int col = i % 3;
            int row = i / 3;
            int id = i;
            this.addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> click(id))
                    .bounds(this.leftPos + 20 + col * 34, this.topPos + 36 + row * 22, 32, 20).build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        Keypad pad = this.getMenu().handler == null ? null : this.getMenu().handler.getKeypad();
        int bg = 0xFF2A2A20;
        if (pad != null && pad.successColorTicks > 0) bg = 0xFF1A4A1A;
        if (pad != null && pad.failColorTicks > 0) bg = 0xFF4A1A1A;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, bg);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 32, 0xFF111111);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Keypad pad = this.getMenu().handler == null ? null : this.getMenu().handler.getKeypad();
        String shown = "------";
        String mode = "SET";
        if (pad != null) {
            char[] digits = new char[6];
            for (int i = 0; i < 6; i++) {
                digits[5 - i] = pad.code[i] < 0 ? '-' : (char) ('0' + pad.code[i]);
            }
            shown = new String(digits);
            mode = pad.isSettingCode ? "SET" : "LOCK";
        }
        guiGraphics.drawString(this.font, mode + "  " + shown, 12, 12, 0x00FF00, false);
    }

    private void click(int id) {
        if (this.getMenu().be == null) return;
        PacketDistributor.sendToServer(new KeypadServerPacket(this.getMenu().be.getBlockPos(), 0, id));
    }
}
