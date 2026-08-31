package com.hbm.inventory.gui;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.packet.toserver.SatPanelActionPayload;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only, containerless GUI for {@link com.hbm.items.tool.ItemSatInterface} (non-coord
 * variant, {@code sat_interface}), ported from CE's {@code GUIScreenSatInterface} (288 lines, now
 * read in full for this review pass - see
 * {@code docs/phase5/gui_screens_survey_weapons_storage_special.md} Headline finding 3, which
 * confirmed this port's earlier version - written before {@code GUIScreenSatInterface} had been
 * read - was a much smaller placeholder than CE's real screen, a "live radar/map minigame", not a
 * static status readout).
 * <p>
 * <b>Rebuilt against CE's exact mechanics</b>:
 * <ul>
 *   <li>Real dimensions: 216x216 (this port's earlier placeholder was 200x150).</li>
 *   <li>A 200x200 map area at local (8,8)-(208,208), rendered directly from the <b>client's own
 *   already-loaded {@link Level}</b> ({@link Minecraft#level}/{@link Level#getBlockState}/
 *   {@link Level#getHeight}) - no server payload carries the pixel data itself, exactly like CE.</li>
 *   <li>{@link Satellite.InterfaceActions#HAS_MAP}: a top-down terrain-color scan. One full
 *   {@code z}-row of 200 columns is (re-)sampled roughly every 15ms via {@link #scanPos}, sweeping
 *   through all 200 rows over ~3 seconds - matching CE's {@code drawMap}/{@code progresScan} timing
 *   exactly. Color comes from {@code BlockState#getMapColor} (this version's closest equivalent to
 *   CE's {@code Material.getMaterialMapColor().colorValue}).</li>
 *   <li>{@link Satellite.InterfaceActions#HAS_RADAR}: entities within a &plusmn;100-block AABB
 *   around the tracked view center, every frame, drawn as 8x8 blips (grey = other, red = hostile
 *   {@link Enemy}, blue = {@link Player}) at CE's exact screen-space formula (an
 *   {@code EntityMissileBaseAdvanced} branch is commented out/{@code TODO} in CE itself, so this
 *   port does not reproduce it either).</li>
 *   <li><b>WASD pans the tracked view center by 50 blocks</b> and resets the scan buffer - CE's own
 *   real navigation mechanic, entirely unbuilt before this pass.</li>
 *   <li>Clicking inside the map (when {@link Satellite.InterfaceActions#CAN_CLICK}) sends the
 *   clicked world coordinate as {@code satClickX}/{@code satClickZ} via
 *   {@link SatPanelActionPayload}, dispatched server-side to {@code Satellite#onClick} - this
 *   port's wiring was already correct (per the survey's own finding); only the click <i>surface</i>
 *   (this real scanned map, replacing a placeholder centered box) and its {@link
 *   HBMSoundHandler#techBleep} feedback were missing.</li>
 *   <li>{@link Satellite.InterfaceActions#SHOW_COORDS}: a raw world X/Z tooltip under the cursor
 *   while hovering the map.</li>
 * </ul>
 * <p>
 * <b>Deliberate removal vs. this port's earlier placeholder</b>: the frequency/satellite-type/color/
 * info-lines/{@code tx}-scratch text readout the previous version drew is dropped - having now read
 * {@code GUIScreenSatInterface} in full, CE's real screen draws <i>no</i> such text anywhere (only
 * the not-connected/no-service banners, the map/radar pixels, and the coords tooltip). Per this
 * report's ground rule that CE is the sole source of truth for visuals, this screen now matches that
 * exactly rather than keeping an invented readout. {@link SatPanelPayload#infoLines()}/{@link
 * SatPanelPayload#tx()} remain defined on the payload (harmless - a different, not-yet-built
 * satellite console screen may legitimately want them); flagged here as an open question for
 * whoever owns that, not resolved by this pass.
 * <p>
 * <b>Not ported (named blocker)</b>: {@link Satellite.InterfaceActions#HAS_ORES} (the ore-scan mode)
 * - blocked on a {@code BedrockOreRegistry}-equivalent ore-scan-color table, confirmed not ported
 * anywhere in this port (survey's Blocked/deferred section). If a satellite declares {@code HAS_ORES}
 * without {@code HAS_MAP}, the map area stays blank - a real, named, non-Phase-5 gap, not a bug in
 * this screen.
 */
public class SatInterfaceScreen extends Screen {

    private static final int WIDTH = 216;
    private static final int HEIGHT = 216;
    private static final int MAP_SIZE = 200;
    private static final int MAP_MARGIN = 8;

    private final int freq;
    private int guiLeft;
    private int guiTop;

    /** Tracked view center - CE's own {@code this.x}/{@code this.z}, initialized to the player's position and panned by WASD. */
    private int x;
    private int z;

    private int[][] map = new int[MAP_SIZE][MAP_SIZE];
    private int scanPos = 0;
    private long lastMilli = 0;

    public SatInterfaceScreen(int freq) {
        super(Component.literal("Satellite Interface"));
        this.freq = freq;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WIDTH) / 2;
        this.guiTop = (this.height - HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            this.x = (int) player.getX();
            this.z = (int) player.getZ();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        var options = Minecraft.getInstance().options;
        boolean panned = false;

        if (options.keyUp.matches(keyCode, scanCode)) {
            this.z -= 50;
            panned = true;
        }
        if (options.keyDown.matches(keyCode, scanCode)) {
            this.z += 50;
            panned = true;
        }
        if (options.keyLeft.matches(keyCode, scanCode)) {
            this.x -= 50;
            panned = true;
        }
        if (options.keyRight.matches(keyCode, scanCode)) {
            this.x += 50;
            panned = true;
        }

        if (panned) {
            this.map = new int[MAP_SIZE][MAP_SIZE];
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(guiLeft, guiTop, guiLeft + WIDTH, guiTop + HEIGHT, 0xC0101010);

        SatPanelPayload latest = SatPanelClientState.LATEST;
        if (latest == null || latest.freq() != freq) {
            guiGraphics.drawCenteredString(this.font, "Not connected.", guiLeft + WIDTH / 2, guiTop + HEIGHT / 2, 0xFF5555);
            return;
        }
        if (latest.satIface() != Satellite.Interfaces.SAT_PANEL.ordinal()) {
            guiGraphics.drawCenteredString(this.font, "No service.", guiLeft + WIDTH / 2, guiTop + HEIGHT / 2, 0xFF5555);
            return;
        }

        int mapX = guiLeft + MAP_MARGIN;
        int mapY = guiTop + MAP_MARGIN;
        int mask = latest.ifaceActionsMask();

        if (SatPanelPayload.hasFlag(mask, Satellite.InterfaceActions.HAS_MAP.ordinal())) {
            drawMap(guiGraphics, mapX, mapY);
        }
        if (SatPanelPayload.hasFlag(mask, Satellite.InterfaceActions.HAS_RADAR.ordinal())) {
            drawRadar(guiGraphics, mapX, mapY);
        }

        if (SatPanelPayload.hasFlag(mask, Satellite.InterfaceActions.SHOW_COORDS.ordinal())
                && mouseX >= mapX && mouseX < mapX + MAP_SIZE && mouseY >= mapY && mouseY < mapY + MAP_SIZE) {
            int worldX = this.x + (mouseX - (mapX + 100));
            int worldZ = this.z + (mouseY - (mapY + 100));
            guiGraphics.renderTooltip(this.font, Component.literal(worldX + " / " + worldZ), mouseX, mouseY);
        }
    }

    /** CE: {@code drawMap()} - samples one z-row of the terrain heightmap per scan tick, MapColor-tinted. */
    private void drawMap(GuiGraphics guiGraphics, int mapX, int mapY) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            int wz = this.z + scanPos - 100;
            for (int i = -100; i < 100; i++) {
                int wx = this.x + i;
                int wy = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                BlockPos pos = new BlockPos(wx, wy, wz);
                int col = level.getBlockState(pos).getMapColor(level, pos).col;
                map[i + 100][scanPos] = 0xFF000000 | (col & 0xFFFFFF);
            }
        }

        paintMap(guiGraphics, mapX, mapY);
        progressScan();
    }

    private void paintMap(GuiGraphics guiGraphics, int mapX, int mapY) {
        for (int px = 0; px < MAP_SIZE; px++) {
            for (int pz = 0; pz < MAP_SIZE; pz++) {
                int color = map[px][pz];
                if (color != 0) {
                    guiGraphics.fill(mapX + px, mapY + pz, mapX + px + 1, mapY + pz + 1, color);
                }
            }
        }
    }

    /** CE: {@code progresScan()} - advances the sweep line one row every ~15ms, wrapping at 200. */
    private void progressScan() {
        long now = System.currentTimeMillis();
        if (lastMilli + 15 < now) {
            lastMilli = now;
            scanPos++;
        }
        if (scanPos >= MAP_SIZE) scanPos -= MAP_SIZE;
    }

    /** CE: {@code drawRadar()} - entities in a &plusmn;100-block column around the tracked view center, every frame. */
    private void drawRadar(GuiGraphics guiGraphics, int mapX, int mapY) {
        Level level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        AABB box = new AABB(this.x - 100, 0, this.z - 100, this.x + 100, 5000, this.z + 100);
        for (Entity e : level.getEntities(player, box)) {
            if (e.getBbWidth() * e.getBbWidth() * e.getBbHeight() < 0.5D) continue;

            int ex = (int) ((e.getX() - this.x) / (100D * 2 + 1) * (MAP_SIZE - 8D)) - 4;
            int ez = (int) ((e.getZ() - this.z) / (100D * 2 + 1) * (MAP_SIZE - 8D)) - 4 - 9;

            int color = 0xFFAAAAAA; // other (CE type 5)
            if (e instanceof Enemy) color = 0xFFFF3333; // hostile mob (CE type 6)
            if (e instanceof Player) color = 0xFF33AAFF; // player (CE type 7)

            int px = mapX + 100 + ex;
            int py = mapY + 109 + ez;
            guiGraphics.fill(px, py, px + 8, py + 8, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SatPanelPayload latest = SatPanelClientState.LATEST;
        if (latest == null || latest.freq() != freq
                || !SatPanelPayload.hasFlag(latest.ifaceActionsMask(), Satellite.InterfaceActions.CAN_CLICK.ordinal())) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int mapX = guiLeft + MAP_MARGIN;
        int mapY = guiTop + MAP_MARGIN;
        if (mouseX >= mapX && mouseX < mapX + MAP_SIZE && mouseY >= mapY && mouseY < mapY + MAP_SIZE) {
            int worldX = this.x + (int) (mouseX - (mapX + 100));
            int worldZ = this.z + (int) (mouseY - (mapY + 100));

            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HBMSoundHandler.techBleep.get(), 1.0F));

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
