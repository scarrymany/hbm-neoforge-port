package com.hbm.inventory.gui;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.packet.toserver.SatPanelActionPayload;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.OptionalInt;

/**
 * Client-only, containerless GUI for {@link com.hbm.items.tool.ItemSatInterface}'s coord variant
 * (registry id {@code sat_coord}), ported from CE's {@code GUIScreenSatCoord} (189 lines, read in
 * full for this review pass) - like {@link SatInterfaceScreen}, reconstructed from the item's
 * dispatch contract: a simpler text-entry-only panel for {@code SAT_COORD} satellites, X/Z always
 * shown and Y shown only when the connected satellite declares {@link Satellite.CoordActions#HAS_Y}
 * (CE's own "Y-coord field disabled by default" contract).
 * <p>
 * Review-pass fixes, all against CE's real {@code GUIScreenSatCoord}: {@code HEIGHT} corrected from
 * 110 to CE's real {@code ySize} of 126, fields now default to the player's current position
 * ({@code initGui()}: {@code xField.setText(player.posX)} etc.), matching CE exactly, {@link #send()}
 * now refuses to send if any relevant field fails to parse as a number (matching CE's
 * {@code NumberUtils.isCreatable} guard), {@link #send()} now plays {@link HBMSoundHandler#techBleep}
 * on a successful send and closes the screen afterward.
 */
public class SatCoordScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/satellites/gui_sat_coord.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 126;

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
        this.yField = new EditBox(this.font, guiLeft + 40, guiTop + 62, 100, 14, Component.literal("Y"));
        this.zField = new EditBox(this.font, guiLeft + 40, guiTop + 94, 100, 14, Component.literal("Z"));

        // CE's GUIScreenSatCoord.initGui() unconditionally pre-fills X/Y/Z from the player's own
        // current position (a "target where I'm standing" convenience) - not from any previously
        // received satellite target, which this port's earlier version incorrectly did instead.
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            this.xField.setValue(Integer.toString((int) player.getX()));
            this.yField.setValue(Integer.toString((int) player.getY()));
            this.zField.setValue(Integer.toString((int) player.getZ()));
        } else {
            this.xField.setValue("0");
            this.yField.setValue("-1");
            this.zField.setValue("0");
        }

        this.addRenderableWidget(this.xField);
        this.addRenderableWidget(this.yField);
        this.addRenderableWidget(this.zField);

        this.addRenderableWidget(Button.builder(Component.literal("Send"), b -> send())
                .bounds(guiLeft + 58, guiTop + 110, 60, 14).build());
    }

    private boolean hasY() {
        SatPanelPayload latest = SatPanelClientState.LATEST;
        return latest != null && latest.freq() == freq
                && SatPanelPayload.hasFlag(latest.coordActionsMask(), Satellite.CoordActions.HAS_Y.ordinal());
    }

    private OptionalInt parse(EditBox field) {
        try {
            return OptionalInt.of(Integer.parseInt(field.getValue().trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /** Refuses to send on invalid input, matching CE's {@code NumberUtils.isCreatable} guard - see this class's own javadoc. */
    private void send() {
        OptionalInt x = parse(xField);
        OptionalInt z = parse(zField);
        boolean needY = hasY();
        OptionalInt y = needY ? parse(yField) : OptionalInt.of(-1);

        if (x.isEmpty() || z.isEmpty() || y.isEmpty()) return;

        CompoundTag data = new CompoundTag();
        data.putInt("satCoordX", x.getAsInt());
        data.putInt("satCoordZ", z.getAsInt());
        if (needY) data.putInt("satCoordY", y.getAsInt());
        PacketDistributor.sendToServer(new SatPanelActionPayload(freq, data));

        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HBMSoundHandler.techBleep.get(), 1.0F));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);
        guiGraphics.drawCenteredString(this.font, this.title, guiLeft + WIDTH / 2, guiTop + 8, 0xFFFFFF);
        guiGraphics.drawString(this.font, "X:", guiLeft + 24, guiTop + 33, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Y:", guiLeft + 24, guiTop + 65, hasY() ? 0xFFFFFF : 0x666666, false);
        guiGraphics.drawString(this.font, "Z:", guiLeft + 24, guiTop + 97, 0xFFFFFF, false);
        this.yField.setEditable(hasY());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
