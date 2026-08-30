package com.hbm.inventory.gui;

import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.packet.toserver.SatPanelActionPayload;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only, containerless GUI for {@link com.hbm.items.tool.ItemSatInterface} (non-coord
 * variant, {@code sat_interface}), ported from CE's {@code GUIScreenSatInterface} - not itself in
 * this pass's read file set (only the item's own {@code provideGUI}/{@code onUpdate} pair was, per
 * {@code docs/phase3/missile_launch_infra.md}'s Phase-3-safe scope table), so its layout here is a
 * reasonable, documented reconstruction from the item's dispatch contract rather than a line-for-line
 * port: a live status readout ({@link SatPanelClientState}, refreshed every 2 server ticks) plus a
 * click-to-target map placeholder for {@code SAT_PANEL} satellites that declare
 * {@link Satellite.InterfaceActions#HAS_MAP}/{@link Satellite.InterfaceActions#CAN_CLICK} - clicking
 * inside the map area sends the clicked world offset as a {@code satClickX}/{@code satClickZ}
 * {@link SatPanelActionPayload}, mirroring CE's own click-dispatch contract
 * ({@code Satellite#onClick}'s "x/z translated from on-screen coords to actual world coordinates").
 */
public class SatInterfaceScreen extends Screen {

    private static final int WIDTH = 200;
    private static final int HEIGHT = 150;
    private static final int MAP_SIZE = 120;

    private final int freq;
    private int guiLeft;
    private int guiTop;

    public SatInterfaceScreen(int freq) {
        super(Component.literal("Satellite Interface"));
        this.freq = freq;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WIDTH) / 2;
        this.guiTop = (this.height - HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(guiLeft, guiTop, guiLeft + WIDTH, guiTop + HEIGHT, 0xC0101010);
        guiGraphics.drawString(this.font, "Freq: " + freq, guiLeft + 8, guiTop + 6, 0xFFFFFF, false);

        SatPanelPayload latest = SatPanelClientState.LATEST;
        if (latest == null || latest.freq() != freq) {
            guiGraphics.drawString(this.font, "No satellite connected.", guiLeft + 8, guiTop + 20, 0xFF5555, false);
            return;
        }

        int color = 0xFF000000
                | ((int) (latest.colorR() * 255) << 16)
                | ((int) (latest.colorG() * 255) << 8)
                | (int) (latest.colorB() * 255);

        guiGraphics.drawString(this.font, latest.satType(), guiLeft + 8, guiTop + 20, color, false);

        boolean hasMap = SatPanelPayload.hasFlag(latest.ifaceActionsMask(), Satellite.InterfaceActions.HAS_MAP.ordinal());
        int mapX = guiLeft + 8;
        int mapY = guiTop + 32;
        if (hasMap) {
            guiGraphics.fill(mapX, mapY, mapX + MAP_SIZE, mapY + MAP_SIZE, 0x60000000);
            int cx = mapX + MAP_SIZE / 2;
            int cy = mapY + MAP_SIZE / 2;
            guiGraphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFF0000);
        }

        int infoY = mapY + (hasMap ? MAP_SIZE + 6 : 0);
        for (String line : latest.infoLines()) {
            guiGraphics.drawString(this.font, line, guiLeft + 8, infoY, 0xCCCCCC, false);
            infoY += 10;
        }

        if (!latest.tx().isEmpty()) {
            guiGraphics.drawString(this.font, "> " + latest.tx(), guiLeft + 8, guiTop + HEIGHT - 12, 0x55FF55, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SatPanelPayload latest = SatPanelClientState.LATEST;
        if (latest == null || latest.freq() != freq) return super.mouseClicked(mouseX, mouseY, button);
        if (!SatPanelPayload.hasFlag(latest.ifaceActionsMask(), Satellite.InterfaceActions.CAN_CLICK.ordinal())) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int mapX = guiLeft + 8;
        int mapY = guiTop + 32;
        if (mouseX >= mapX && mouseX < mapX + MAP_SIZE && mouseY >= mapY && mouseY < mapY + MAP_SIZE) {
            // Map is centered on the satellite's current target; each pixel represents 1 block.
            int worldX = latest.targetX() + (int) (mouseX - (mapX + MAP_SIZE / 2D));
            int worldZ = latest.targetZ() + (int) (mouseY - (mapY + MAP_SIZE / 2D));

            CompoundTag data = new CompoundTag();
            data.putInt("satClickX", worldX);
            data.putInt("satClickZ", worldZ);
            PacketDistributor.sendToServer(new SatPanelActionPayload(freq, data));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
