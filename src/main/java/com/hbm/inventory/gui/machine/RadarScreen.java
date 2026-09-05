package com.hbm.inventory.gui.machine;

import com.hbm.api.entity.RadarEntry;
import com.hbm.blockentity.machine.MachineRadarBlockEntity;
import com.hbm.inventory.container.machine.RadarMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

/**
 * Slots 0–7 + keys 1–8 on a contact → {@code NBTControlPacket} Exact CE
 * {@code GUIMachineRadarNT.java:230-257} (map png skipped; coords from the hovered entry).
 * Scan toggles Exact CE {@code GUIMachineRadarNT.java:56-71} / {@code :91-95}
 * (grey 8×8 at CE −10,88…128 — no invented {@code gui_radar_nt.png}).
 */
public class RadarScreen extends GuiInfoContainer<RadarMenu> {

    private static final int CONTACT_X = 32;
    private static final int CONTACT_Y = 70;
    private static final int CONTACT_LINE = 10;
    private static final int CONTACT_MAX = 3;

    /** CE {@code GUIMachineRadarNT.java:56-60} — skip map/gui1/clear. */
    private static final String[] TOGGLE_KEYS = {"missiles", "shells", "players", "smart", "red"};
    private static final String[] TOGGLE_I18N = {
            "radar.detectMissiles", "radar.detectShells", "radar.detectPlayers",
            "radar.smartMode", "radar.redMode"
    };
    private static final int TOGGLE_X = -10;
    private static final int TOGGLE_Y0 = 88;
    private static final int TOGGLE_SIZE = 8;
    private static final int TOGGLE_STEP = 10;

    private int lastMouseX;
    private int lastMouseY;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 212;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        int power = (int) (88L * this.getMenu().be.getPower() / Math.max(1, this.getMenu().be.getMaxPower()));
        guiGraphics.fill(x + 8, y + 18 + (88 - power), x + 24, y + 18 + 88, 0xFFFF0000);

        MachineRadarBlockEntity be = this.getMenu().be;
        boolean[] on = {be.scanMissiles, be.scanShells, be.scanPlayers, be.smartMode, be.redMode};
        for (int i = 0; i < TOGGLE_KEYS.length; i++) {
            int tx = x + TOGGLE_X;
            int ty = y + TOGGLE_Y0 + i * TOGGLE_STEP;
            guiGraphics.fill(tx, ty, tx + TOGGLE_SIZE, ty + TOGGLE_SIZE, 0xFF555555);
            if (on[i]) {
                guiGraphics.fill(tx + 1, ty + 1, tx + TOGGLE_SIZE - 1, ty + TOGGLE_SIZE - 1, 0xFF55FF55);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRadarBlockEntity be = this.getMenu().be;
        guiGraphics.drawString(this.font, "Contacts: " + be.getContacts(), 32, 20, 0x404040, false);
        guiGraphics.drawString(this.font, "Redstone: " + be.getRedPower(), 32, 32, 0x404040, false);
        guiGraphics.drawString(this.font, "1-8 launch", 32, 42, 0x404040, false);
        guiGraphics.drawString(this.font, "Linker", 26, 98, 0x404040, false);
        List<RadarEntry> list = be.entries;
        for (int i = 0; i < Math.min(list.size(), CONTACT_MAX); i++) {
            RadarEntry e = list.get(i);
            String name = e.unlocalizedName == null ? "?" : e.unlocalizedName;
            guiGraphics.drawString(this.font, name + " " + e.posX + "/" + e.posZ, CONTACT_X, CONTACT_Y + i * CONTACT_LINE, 0x404040, false);
        }
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 88, be.getPower(), be.getMaxPower());
        for (int i = 0; i < TOGGLE_I18N.length; i++) {
            Component[] lines = Arrays.stream(I18nUtil.resolveKeyArray(TOGGLE_I18N[i]))
                    .map(Component::literal).toArray(Component[]::new);
            drawCustomInfo(guiGraphics, mouseX, mouseY,
                    leftPos + TOGGLE_X, topPos + TOGGLE_Y0 + i * TOGGLE_STEP,
                    TOGGLE_SIZE, TOGGLE_SIZE, lines);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMachineRadarNT.java:56-71
        for (int i = 0; i < TOGGLE_KEYS.length; i++) {
            if (isHovered(mouseX, mouseY, TOGGLE_X, TOGGLE_Y0 + i * TOGGLE_STEP, TOGGLE_SIZE, TOGGLE_SIZE)) {
                click();
                CompoundTag data = new CompoundTag();
                data.putBoolean(TOGGLE_KEYS[i], true);
                PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // CE GUIMachineRadarNT.java:230-257 — keys 1-8
        if (codePoint >= '1' && codePoint <= '8') {
            RadarEntry hovered = hoveredEntry(lastMouseX, lastMouseY);
            if (hovered != null) {
                CompoundTag data = new CompoundTag();
                data.putInt("launchEntity", hovered.entityID);
                data.putInt("launchPosX", hovered.posX);
                data.putInt("launchPosZ", hovered.posZ);
                data.putInt("link", codePoint - '1');
                PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private RadarEntry hoveredEntry(int mouseX, int mouseY) {
        List<RadarEntry> list = this.getMenu().be.entries;
        int x = this.leftPos + CONTACT_X;
        int y = this.topPos + CONTACT_Y;
        for (int i = 0; i < Math.min(list.size(), CONTACT_MAX); i++) {
            int top = y + i * CONTACT_LINE;
            if (mouseX >= x && mouseX < x + 130 && mouseY >= top && mouseY < top + CONTACT_LINE) {
                return list.get(i);
            }
        }
        return null;
    }
}
