package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKColumn;
import com.hbm.blockentity.machine.rbmk.RBMKColumn.ColumnType;
import com.hbm.inventory.container.machine.rbmk.RBMKConsoleMenu;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * Playable console GUI. Click coords Exact CE {@code GUIRBMKConsole.java:127-300}.
 * No invented {@code gui_rbmk_console.png} — filled rects, not atlas tiles. TESR stay skipped.
 */
public class RBMKConsoleScreen extends AbstractContainerScreen<RBMKConsoleMenu> {

    private final boolean[] selection = new boolean[15 * 15];
    private boolean az5Lid = true;
    private long lastPress;
    private EditBox field;

    public RBMKConsoleScreen(RBMKConsoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 244;
        this.imageHeight = 172;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 4;
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(this.font, leftPos + 9, topPos + 84, 35, 9, Component.empty());
        this.field.setTextColor(0x00FF00);
        this.field.setTextColorUneditable(0x008000);
        this.field.setBordered(false);
        this.field.setMaxLength(3);
        this.field.setValue("0");
        this.addRenderableWidget(this.field);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);

        final int bX = 86;
        final int bY = 11;
        final int size = 10;
        RBMKColumn[] cols = menu.be.columns;
        for (int i = 0; i < cols.length; i++) {
            int x = leftPos + bX + size * (i % 15);
            int y = topPos + bY + size * (i / 15);
            RBMKColumn col = cols[i];
            int color = col == null ? 0xFF111111 : colorFor(col.type);
            graphics.fill(x, y, x + size - 1, y + size - 1, color);
            if (this.selection[i]) {
                graphics.fill(x, y, x + size - 1, y + 1, 0xFFFFFFFF);
                graphics.fill(x, y + size - 2, x + size - 1, y + size - 1, 0xFFFFFFFF);
                graphics.fill(x, y, x + 1, y + size - 1, 0xFFFFFFFF);
                graphics.fill(x + size - 2, y, x + size - 1, y + size - 1, 0xFFFFFFFF);
            }
        }

        // CE button hitboxes — painted so they are usable without the atlas
        fillBtn(graphics, 61, 70, 10, 10, 0xFF886600); // select all rods
        fillBtn(graphics, 72, 70, 10, 10, 0xFF555555); // deselect
        int[] group = {0xFFAA0000, 0xFFAAAA00, 0xFF00AA00, 0xFF0000AA, 0xFFAA00AA};
        for (int k = 0; k < 5; k++) fillBtn(graphics, 6 + k * 11, 70, 10, 10, group[k]);
        fillBtn(graphics, 70, 82, 12, 12, 0xFF6688AA); // compressor
        fillBtn(graphics, 48, 82, 12, 12, 0xFF228822); // apply level
        fillBtn(graphics, 30, 138, 28, 28, az5Lid ? 0xFF884400 : 0xFFCC0000); // AZ-5
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 2; k++) {
                fillBtn(graphics, 6 + 40 * k, 8 + 21 * j, 18, 18, 0xFF444466);
                fillBtn(graphics, 24 + 40 * k, 8 + 21 * j, 18, 18, 0xFF446644);
            }
        }
    }

    private void fillBtn(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, color);
    }

    private static int colorFor(ColumnType type) {
        return switch (type) {
            case FUEL, FUEL_SIM -> 0xFF2E7D32;
            case CONTROL -> 0xFFC62828;
            case CONTROL_AUTO -> 0xFFE64A19;
            case BOILER -> 0xFF1565C0;
            case MODERATOR -> 0xFF6A1B9A;
            case ABSORBER -> 0xFF37474F;
            case REFLECTOR -> 0xFF90A4AE;
            case OUTGASSER -> 0xFF00838F;
            case COOLER -> 0xFF00ACC1;
            case HEATEX -> 0xFFEF6C00;
            case STORAGE -> 0xFF5D4037;
            default -> 0xFF616161;
        };
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 4, 0xFFFFFF, false);
        int[] buf = menu.be.fluxBuffer;
        int last = buf.length > 0 ? buf[buf.length - 1] : 0;
        graphics.drawString(this.font, "flux " + last, 8, 158, 0x00FF00, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.field.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.field);
            return true;
        }

        final int LEFT = 0;
        final int RIGHT = 1;
        final int bX = 86;
        final int bY = 11;
        final int size = 10;

        // CE :139-147 — toggle column selection
        if (mouseX >= leftPos + 86 && mouseX < leftPos + 86 + 150 && mouseY >= topPos + 11 && mouseY < topPos + 11 + 150) {
            int index = (int) ((mouseX - bX - leftPos) / size + (mouseY - bY - topPos) / size * 15);
            if (index >= 0 && index < selection.length && menu.be.columns[index] != null) {
                this.selection[index] = !this.selection[index];
                return true;
            }
        }

        // CE :151-154 — clear
        if (hit(mouseX, mouseY, 72, 70, 10, 10)) {
            java.util.Arrays.fill(this.selection, false);
            return true;
        }

        // CE :158-167 — select all CONTROL
        if (hit(mouseX, mouseY, 61, 70, 10, 10)) {
            java.util.Arrays.fill(this.selection, false);
            RBMKColumn[] cols = menu.be.columns;
            for (int j = 0; j < cols.length; j++) {
                if (cols[j] != null && cols[j].type == ColumnType.CONTROL) this.selection[j] = true;
            }
            return true;
        }

        // CE :171-185 — compressor
        if (hit(mouseX, mouseY, 70, 82, 12, 12)) {
            CompoundTag control = new CompoundTag();
            control.putBoolean("compressor", true);
            control.putIntArray("cols", selectedOf(ColumnType.BOILER));
            PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), control));
            return true;
        }

        // CE :189-216 — color groups
        for (int k = 0; k < 5; k++) {
            if (hit(mouseX, mouseY, 6 + k * 11, 70, 10, 10)) {
                if (button == LEFT) {
                    java.util.Arrays.fill(this.selection, false);
                    RBMKColumn[] cols = menu.be.columns;
                    for (int j = 0; j < cols.length; j++) {
                        if (cols[j] != null && cols[j].type == ColumnType.CONTROL
                                && cols[j] instanceof RBMKColumn.ControlColumn ctrl && ctrl.color == k) {
                            this.selection[j] = true;
                        }
                    }
                } else if (button == RIGHT) {
                    CompoundTag control = new CompoundTag();
                    control.putByte("assignColor", (byte) k);
                    control.putIntArray("cols", selectedOf(ColumnType.CONTROL));
                    PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), control));
                }
                return true;
            }
        }

        // CE :220-239 — AZ-5 (level 0 all CONTROL). Cover audio skipped.
        if (hit(mouseX, mouseY, 30, 138, 28, 28)) {
            if (az5Lid) {
                az5Lid = false;
            } else if (lastPress + 3000 < System.currentTimeMillis()) {
                lastPress = System.currentTimeMillis();
                CompoundTag control = new CompoundTag();
                control.putDouble("level", 0);
                RBMKColumn[] cols = menu.be.columns;
                for (int j = 0; j < cols.length; j++) {
                    if (cols[j] != null && cols[j].type == ColumnType.CONTROL) control.putInt("sel_" + j, j);
                }
                PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), control));
            }
            return true;
        }

        // CE :243-265 — apply field % to selection
        if (hit(mouseX, mouseY, 48, 82, 12, 12)) {
            try {
                int j = (int) Mth.clamp(Double.parseDouble(field.getValue()), 0, 100);
                field.setValue(j + "");
                CompoundTag control = new CompoundTag();
                control.putDouble("level", j * 0.01D);
                for (int s = 0; s < selection.length; s++) {
                    if (selection[s]) control.putInt("sel_" + s, s);
                }
                PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), control));
            } catch (NumberFormatException ignored) {
                return true;
            }
            return true;
        }

        // CE :269-299 — screen toggle / assign
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 2; k++) {
                int slot = j * 2 + k;
                if (hit(mouseX, mouseY, 6 + 40 * k, 8 + 21 * j, 18, 18)) {
                    CompoundTag control = new CompoundTag();
                    control.putByte("toggle", (byte) slot);
                    PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), control));
                    return true;
                }
                if (hit(mouseX, mouseY, 24 + 40 * k, 8 + 21 * j, 18, 18)) {
                    CompoundTag control = new CompoundTag();
                    control.putByte("id", (byte) slot);
                    for (int s = 0; s < selection.length; s++) {
                        if (selection[s]) control.putBoolean("s" + s, true);
                    }
                    PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), control));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private int[] selectedOf(ColumnType type) {
        java.util.List<Integer> ints = new java.util.ArrayList<>();
        RBMKColumn[] cols = menu.be.columns;
        for (int j = 0; j < cols.length; j++) {
            if (cols[j] != null && cols[j].type == type && this.selection[j]) ints.add(j);
        }
        int[] out = new int[ints.size()];
        for (int i = 0; i < out.length; i++) out[i] = ints.get(i);
        return out;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.field.keyPressed(keyCode, scanCode, modifiers) || this.field.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.field.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
