package com.hbm.inventory.gui;

import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.packet.toserver.SatPanelActionPayload;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only, containerless GUI for {@link com.hbm.items.tool.ItemSatInterface}'s coord variant
 * (registry id {@code sat_coord}), ported from CE's {@code GUIScreenSatCoord} - like
 * {@link SatInterfaceScreen}, reconstructed from the item's dispatch contract (see that class's
 * javadoc for why): a simpler text-entry-only panel for {@code SAT_COORD} satellites, X/Z always
 * shown and Y shown only when the connected satellite declares
 * {@link Satellite.CoordActions#HAS_Y} (CE's own "Y-coord field disabled by default" contract).
 */
public class SatCoordScreen extends Screen {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 110;

    private final int freq;
    private int guiLeft;
    private int guiTop;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public SatCoordScreen(int freq) {
        super(Component.literal("Satellite Coordinate Remote"));
        this.freq = freq;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WIDTH) / 2;
        this.guiTop = (this.height - HEIGHT) / 2;

        this.xField = new EditBox(this.font, guiLeft + 40, guiTop + 30, 100, 14, Component.literal("X"));
        this.xField.setValue("0");
        this.addRenderableWidget(this.xField);

        this.yField = new EditBox(this.font, guiLeft + 40, guiTop + 46, 100, 14, Component.literal("Y"));
        this.yField.setValue("-1");
        this.addRenderableWidget(this.yField);

        this.zField = new EditBox(this.font, guiLeft + 40, guiTop + 62, 100, 14, Component.literal("Z"));
        this.zField.setValue("0");
        this.addRenderableWidget(this.zField);

        this.addRenderableWidget(Button.builder(Component.literal("Send"), b -> send())
                .bounds(guiLeft + 58, guiTop + 82, 60, 18).build());

        SatPanelPayload latest = SatPanelClientState.LATEST;
        if (latest != null && latest.freq() == freq) {
            this.xField.setValue(Integer.toString(latest.targetX()));
            this.zField.setValue(Integer.toString(latest.targetZ()));
        }
    }

    private boolean hasY() {
        SatPanelPayload latest = SatPanelClientState.LATEST;
        return latest != null && latest.freq() == freq
                && SatPanelPayload.hasFlag(latest.coordActionsMask(), Satellite.CoordActions.HAS_Y.ordinal());
    }

    private int parse(EditBox field, int fallback) {
        try {
            return Integer.parseInt(field.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void send() {
        CompoundTag data = new CompoundTag();
        data.putInt("satCoordX", parse(xField, 0));
        data.putInt("satCoordZ", parse(zField, 0));
        if (hasY()) data.putInt("satCoordY", parse(yField, -1));
        PacketDistributor.sendToServer(new SatPanelActionPayload(freq, data));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(guiLeft, guiTop, guiLeft + WIDTH, guiTop + HEIGHT, 0xC0101010);
        guiGraphics.drawCenteredString(this.font, this.title, guiLeft + WIDTH / 2, guiTop + 8, 0xFFFFFF);
        guiGraphics.drawString(this.font, "X:", guiLeft + 24, guiTop + 33, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Y:", guiLeft + 24, guiTop + 49, hasY() ? 0xFFFFFF : 0x666666, false);
        guiGraphics.drawString(this.font, "Z:", guiLeft + 24, guiTop + 65, 0xFFFFFF, false);
        this.yField.setEditable(hasY());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
